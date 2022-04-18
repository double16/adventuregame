package org.patdouble.adventuregame.engine

import groovy.transform.CompileDynamic
import groovy.transform.PackageScope
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.drools.core.common.InternalAgenda
import org.kie.api.KieServices
import org.kie.api.logger.KieRuntimeLogger
import org.kie.api.runtime.KieContainer
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.KieSessionConfiguration
import org.kie.api.runtime.rule.FactHandle
import org.kie.api.runtime.rule.QueryResults
import org.kie.api.runtime.rule.QueryResultsRow
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory
import org.kie.internal.runtime.conf.ForceEagerActivationOption
import org.patdouble.adventuregame.engine.action.ActionExecutor
import org.patdouble.adventuregame.engine.state.Helpers
import org.patdouble.adventuregame.engine.state.StoryState
import org.patdouble.adventuregame.flow.ChronosChanged
import org.patdouble.adventuregame.flow.GoalFulfilled
import org.patdouble.adventuregame.flow.Notification
import org.patdouble.adventuregame.flow.StoryEnded
import org.patdouble.adventuregame.flow.PlayerChanged
import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.RequestSatisfied
import org.patdouble.adventuregame.flow.StoryMessage
import org.patdouble.adventuregame.i18n.ActionStatement
import org.patdouble.adventuregame.i18n.ActionStatementParser
import org.patdouble.adventuregame.i18n.Bundles
import org.patdouble.adventuregame.model.Action

import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.state.Chronos
import org.patdouble.adventuregame.state.Event
import org.patdouble.adventuregame.state.GoalStatus
import org.patdouble.adventuregame.state.History
import org.patdouble.adventuregame.state.KieMutableProperties
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.PlayerEvent
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.ActionRequest
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.state.request.Request

import javax.validation.constraints.NotNull
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Flow
import java.util.concurrent.SubmissionPublisher
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs the story by advancing the {@link Chronos}, declaring necessary inputs from humans, meeting goals for AI
 * players, etc.
 * Responsible for updating the history.
 *
 * The entire state is stored in the {@link Story} object.
 */
@ToString(includes = ['story', 'locale', 'autoLifecycle'])
@Slf4j
@CompileDynamic
@SuppressWarnings('CatchRuntimeException')
class Engine {
    @Lazy
    private static final Executor DEFAULT_EXECUTOR = { Executors.newCachedThreadPool() } ()

    /**
     * Count of engines in the process of closing.
     */
    static final AtomicInteger PENDING_CLOSE = new AtomicInteger(0)

    Story story
    final Locale locale = Locale.ENGLISH
    final ActionStatementParser actionStatementParser
    final Bundles bundles
    final EngineFacade facade
    /** Automatically move through the lifecycle. */
    boolean autoLifecycle
    /** Stops the engine if the chronos reaches this value, prevents run away execution. */
    Integer chronosLimit

    private final Executor executor
    @Delegate(interfaces = false, includeTypes = [Flow.Publisher])
    final private SubmissionPublisher<StoryMessage> publisher

    private final DroolsConfiguration droolsConfiguration
    private KieContainer kContainer
    private KieSession kieSession
    private final AtomicBoolean halting = new AtomicBoolean(false)
    private KieRuntimeLogger kieRuntimeLogger
    File kieRuntimeLoggerFile

    private final AtomicBoolean firingLatch = new AtomicBoolean(false)
    private volatile CompletableFuture<Boolean> firingComplete
    private final CompletableFuture<Void> storyEnd = new CompletableFuture<>()

    private final AtomicBoolean initKieSessionLatch = new AtomicBoolean(false)
    private FactHandle chronosHandle
    private FactHandle storyStateHandle
    private final Map<UUID, FactHandle> handles = [:]

    /**
     * Holds the actions that have occurred during this chronos. After all processing is done, these actions will be
     * recorded in history with the resulting player state. It's likely the actions of other players will affect this
     * player so we need to wait until all actions are processed before creating an event.
     */
    private final Map<UUID, ActionStatement> currentActions = [:]

    @SuppressWarnings('ThisReferenceEscapesConstructor')
    Engine(@NotNull Story story, Executor executor = null, Executor flowExecutor = null) {
        Objects.requireNonNull(story)
        this.facade = new EngineFacade(this)
        this.story = story
        if (this.story.ended) {
            storyEnd.complete(null)
        }
        this.executor = executor ?: DEFAULT_EXECUTOR
        this.publisher = new SubmissionPublisher<>(flowExecutor ?: this.executor, Flow.defaultBufferSize())
        actionStatementParser = new ActionStatementParser(locale)
        bundles = Bundles.get(locale)
        droolsConfiguration = new DroolsConfiguration()
        newKieSession()
        Objects.requireNonNull(kieSession)
    }

    /**
     * Initialize the story to be ready to play, which includes creating request for players.
     */
    CompletableFuture<Void> init() {
        assert story.world

        final CompletableFuture<Void> future = new CompletableFuture<>()
        kieSession.submit { KieSession kieSession ->
            if (story.chronos == null) {
                story.chronos = new Chronos()
                story.history = new History(story.world)
                createPlayerRequests()
                createGoalStatus()
                checkInitComplete(future)
            } else {
                future.complete(null)
            }
        }
        future
    }

    /**
     * Replace story object, which is expected to happen due to persistence behavior. This is NOT intended to change the
     * content of the story, only the objects involved.
     */
    CompletableFuture<Story> updateStory(Closure<Story> storyProducer) {
        Objects.requireNonNull(storyProducer)
        if (kieSession == null ) { // ended
            return CompletableFuture.completedFuture(story)
        }
        final CompletableFuture<Story> future = new CompletableFuture<>()
        kieSession.submit { KieSession kieSession ->
            Story story = storyProducer.call()
            if (story == null) {
                future.complete(story)
                return
            }
            if (story.is(this.story)) {
                future.complete(story)
            } else {
                this.story = story
                if (storyStateHandle != null) {
                    populateKieSession(kieSession)
                    syncStoryState(kieSession)
                }
                future.complete(story)
            }
        }
        future
    }

    /**
     * Adds a player to satisfy a {@link PlayerRequest}.
     * @throw IllegalArgumentException if a matching PlayerRequest isn't found.
     */
    @SuppressWarnings('Instanceof')
    CompletableFuture<Void> addToCast(Player player) {
        final CompletableFuture<Void> future = new CompletableFuture<>()
        kieSession.submit { KieSession kieSession ->
            try {
                doAddToCast(player)
                checkInitComplete(future)
            } catch (RuntimeException e) {
                future.completeExceptionally(e)
            }
        }
        future
    }

    /**
     * Ignore the request.
     * @throws IllegalArgumentException if the request isn't optional
     */
    CompletableFuture<Void> ignore(PlayerRequest request) {
        final CompletableFuture<Void> future = new CompletableFuture<>()
        if (request.optional) {
            kieSession.submit { KieSession kieSession ->
                try {
                    doIgnore(request)
                    checkInitComplete(future)
                } catch (RuntimeException e) {
                    future.completeExceptionally(e)
                }
            }
        } else {
            future.completeExceptionally(new IllegalArgumentException(
                    "Can not ignore required player ${request.template}"))
        }
        future
    }

    /**
     * Start the game after the required cast members have been satisfied. Any requests for optional p[layers will be
     * removed from the story by this method.
     * @param forceMotivator if set, any remaining required players will be created with the specified motivator
     * @throew IllegalStateException if there are required players not yet cast
     */
    @SuppressWarnings('Instanceof')
    CompletableFuture<Void> start(final Motivator forceMotivator = null) {
        final CompletableFuture<Void> future = new CompletableFuture<>()
        kieSession.submit { KieSession kieSession ->
            try {
                if (forceMotivator == null) {
                    List<PlayerRequest> pendingPlayers = story.requests
                            .findAll { it instanceof PlayerRequest && !it.optional }
                    if (!pendingPlayers.empty) {
                        String msg = bundles.requiredPlayersTemplate
                                .make([names: pendingPlayers*.template*.fullName]) as String
                        publisher.submit(new Notification(
                                bundles.text.getString('state.players_required.subject'),
                                msg))
                        throw new IllegalStateException(msg)
                    }
                }

                story.requests.findAll { it instanceof PlayerRequest }.each { PlayerRequest playerRequest ->
                    if (playerRequest.optional) {
                        doIgnore(playerRequest)
                    } else {
                        doAddToCast(playerRequest.template.createPlayer(forceMotivator))
                    }
                }

                placePlayers()
                initKieSession()
            } catch (RuntimeException e) {
                future.completeExceptionally(e)
            }
        }
        // we need to ensure the previous atomic operation is complete before calling fireUntilHalt()
        kieSession.submit {
            if (future.isCompletedExceptionally()) {
                return
            }
            try {
                startRuleEngine(future)
            } catch (RuntimeException e) {
                future.completeExceptionally(e)
            }
        }
        if (autoLifecycle) {
            future
        } else {
            future.thenRun { next() }
        }
    }

    /**
     * Progress the story. Only necessary when {@link #autoLifecycle} is false. Otherwise this is a NOP.
     */
    void next() {
        CompletableFuture<Boolean> firingComplete = this.firingComplete
        if (firingComplete == null || firingComplete.isDone()) {
            kieSession?.fireAllRules()
        }
    }

    CompletableFuture<Boolean> getFiringComplete() {
        this.firingComplete
    }

    CompletableFuture<Void> getStoryEnd() {
        this.storyEnd
    }

    /**
     * End the story.
     */
    CompletableFuture<Void> end() {
        // It's possible for the Engine to end but rule activation are still scheduled to fire
        final KieSession kieSessionHolder = kieSession
        if (kieSessionHolder == null) {
            return CompletableFuture.completedFuture(null)
        }

        final CompletableFuture<Void> future = new CompletableFuture<>()
        kieSessionHolder.submit { KieSession kieSession ->
            if (!story.ended) {
                story.ended = true
                recordHistory()
                syncStoryState(kieSession)
                removeKieObjects(kieSession, story.requests)
                story.requests.clear()
                publisher.submit(new StoryEnded())
            }
            future.complete(null)
            storyEnd.complete(null)
        }
        future
    }

    /**
     * Close and cleanup the engine. This does not end the story.
     */
    CompletableFuture<Void> close() throws IOException {
        final UUID id = story.id
        log.info 'Closing Engine for story {}', id
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null)
        PENDING_CLOSE.incrementAndGet()
        KieSession kieSession1 = kieSession
        if (kieSession1) {
            kieSession = null
            future = future.thenRun {
                log.info 'Halting rule engine {}', id
                halting.set(true)
                kieSession1.halt()
            }
            CompletableFuture<Boolean> firingComplete = this.firingComplete
            if (firingComplete != null) {
                future = future.thenCombine(firingComplete, { a,b -> null })
            }
            future = future.thenRun {
                log.info 'Disposing of rule engine {}', id
                kieSession1.dispose()
                if (kieRuntimeLogger != null) {
                    kieRuntimeLogger.close()
                    kieRuntimeLogger = null
                }
            }
        }
        future = future.thenRun {
            if (!publisher.isClosed()) {
                log.info 'Closing message publisher {}', id
                publisher.close()
            }
            PENDING_CLOSE.decrementAndGet()
        }
        future
    }

    /**
     * Check if this engine is closed, i.e. it is no longer running a story. This does not indicate the story has
     * ended.
     */
    boolean isClosed() {
        publisher.isClosed()
    }

    /**
     * Wait for the rule engine to stop firing.
     * @param max the maximum amount of time to wait
     */
    void waitForFiringFinish(Duration max) {
        long end = System.currentTimeMillis() + max.toMillis()
        while (isFiring() && !Thread.interrupted() && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(200)
            } catch (InterruptedException e) {
                break
            }
        }
    }

    /**
     * Publish a message.
     * @param storyMessage
     *  @return the estimated maximum lag among subscribers
     */
    int submit(StoryMessage storyMessage) {
        publisher.submit(storyMessage)
    }

    /**
     * Mark a goal as fulfilled.
     */
    CompletableFuture<Void> fulfill(GoalStatus goal) {
        // It's possible for the Engine to end but rule activation are still scheduled to fire
        final KieSession kieSessionHolder = kieSession
        if (kieSessionHolder == null) {
            return CompletableFuture.completedFuture(null)
        }

        final CompletableFuture<Void> future = new CompletableFuture<>()
        kieSessionHolder.submit { KieSession kieSession ->
            try {
                if (!goal.fulfilled) {
                    goal.fulfilled = true
                    kieSession.update(handles.get(goal.id), goal, 'fulfilled')
                    publisher.submit(new GoalFulfilled(goal.goal))
                }
                future.complete(null)
            } catch (Exception e) {
                future.completeExceptionally(e)
            }
        }
        future
    }

    /**
     * Request the player performs an action.
     * @return true if the action was successful, false otherwise. Any error message will be sent via flow.
     */
    CompletableFuture<Boolean> action(Player player, String statement) {
        action(player, (ActionStatement) actionStatementParser.parse(statement))
    }

    /**
     * Request the player performs an action.
     * @return true if the action was successful, false otherwise. Any error message will be sent via flow.
     */
    @SuppressWarnings('Instanceof')
    CompletableFuture<Boolean> action(Player player, ActionStatement action) {
        log.debug('action for player {} - {}', player, action)

        // It's possible for the Engine to end but rule activation are still scheduled to fire
        final KieSession kieSessionHolder = kieSession
        if (kieSessionHolder == null) {
            return CompletableFuture.completedFuture(false)
        }

        final CompletableFuture<Boolean> future = new CompletableFuture<>()

        kieSessionHolder.submit { KieSession kieSession ->
            try {

                ActionRequest actionRequest = story.requests.find { it instanceof ActionRequest && it.player == player }
                if (player.motivator == Motivator.HUMAN) {
                    if (!actionRequest) {
                        log.error('action for player {} without request, action = {}', player, action)
                        Objects.requireNonNull(player.id)
                        publisher.submit(new PlayerNotification(player,
                                bundles.text.getString('action.norequest.subject'),
                                bundles.text.getString('action.norequest.text')))
                        future.complete(false)
                        return
                    }
                }

                boolean success = false
                boolean validAction = false
                int chronosCost = 0

                if (player.motivator == Motivator.AI_HINT) {
                    submit(new PlayerNotification(player,
                            bundles.getActions().getString('action.hint.notification.subject'),
                            action.text))
                    player.motivator = Motivator.HUMAN
                    addOrReplaceKieObject(kieSession, player, ['motivator'] as String[])
                    validAction = true
                    success = false
                } else {
                    Action builtInAction = action?.getVerbAsAction()
                    if (builtInAction) {
                        ActionExecutor actionInstance = builtInAction.actionClass.getConstructor().newInstance()
                        try {
                            facade.KIE_SESSION_HOLDER.set(kieSession)
                            success = actionInstance.execute(facade, player, action)
                            validAction = true
                            if (success) {
                                chronosCost = action.chronosCost
                            }
                        } catch (UnsupportedOperationException e) {
                            validAction = false
                        } finally {
                            facade.KIE_SESSION_HOLDER.set(null)
                        }
                    } else if (action?.verb) {
                        // TODO: custom actions
                        log.warn('custom actions not yet supported: {}', action.verb)
                    }
                }

                if (!validAction) {
                    Objects.requireNonNull(player.id)
                    publisher.submit(new PlayerNotification(player,
                            bundles.text.getString('action.invalid.subject'),
                            bundles.actionInvalidTextTemplate.make([
                                    actions: actionStatementParser.findAvailableActions(facade, player)
                            ]).toString()))
                    if (actionRequest) {
                        Objects.requireNonNull(actionRequest.id)
                        publisher.submit(new RequestCreated(actionRequest))
                    }
                } else if (success && chronosCost > 0) {
                    player.chronos += chronosCost
                    addOrReplaceKieObject(kieSession, player, ['chronos'] as String[])
                    publisher.submit(new PlayerChanged(player.cloneKeepId(), player.chronos))
                    currentActions.put(player.id, action)

                    if (actionRequest) {
                        story.requests.remove(actionRequest)
                        removeKieObject(kieSession, actionRequest)
                        Objects.requireNonNull(actionRequest.id)
                        publisher.submit(new RequestSatisfied(actionRequest, action))
                    }
                } else {
                    if (actionRequest) {
                        Objects.requireNonNull(actionRequest.id)
                        publisher.submit(new RequestCreated(actionRequest))
                    }
                }

                future.complete(success)
            } catch (RuntimeException e) {
                future.completeExceptionally(e)
            }
        }

        future.thenApply { s ->
            // result of actions may have ended the story
            next()
            return s
        }
    }

    /**
     * Get a list of rooms known to the player.
     */
    CompletableFuture<Collection<Room>> findRoomsKnownToPlayer(Player p) {
        final CompletableFuture<Collection<Room>> future = new CompletableFuture<>()
        executor.execute {
            try {
                QueryResults results = queryRuleEngine('knownRoomsToPlayer', p)
                Collection<Room> rooms = results.collect { QueryResultsRow row -> ((Room) row.get('$room')) }
                future.complete(rooms)
            } catch (RuntimeException e) {
                log.error 'findRoomsKnownToPlayer({})', p, e
                future.completeExceptionally(e)
            }
        }
        future
    }

    void setKieRuntimeLoggerFile(File file) {
        if (kieRuntimeLoggerFile && file) {
            if (kieRuntimeLoggerFile.absolutePath != file.absolutePath) {
                throw new IllegalArgumentException(
                        "Attempt to change KIE log file from ${kieRuntimeLoggerFile} to ${file}")
            }
        }
        if (file) {
            kieRuntimeLoggerFile = file
            kieRuntimeLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(kieSession,
                    kieRuntimeLoggerFile.absolutePath)
        }
    }

    /**
     * Create a new KieSession, configured for the story.
     */
    private void newKieSession() {
        assert story?.world
        if (kieSession != null) {
            return
        }

        kContainer = droolsConfiguration.kieContainer(story.world)

        KieSessionConfiguration ksConfig = KieServices.Factory.get().newKieSessionConfiguration()
        ksConfig.setOption(ForceEagerActivationOption.YES)
        kieSession = kContainer.newKieSession(ksConfig)

        // setup the audit logging
//        KnowledgeRuntimeLoggerFactory.newConsoleLogger(kieSession)
//        KnowledgeRuntimeLoggerFactory.newFileLogger(kieSession, '/tmp/kie')
        if (kieRuntimeLoggerFile) {
            kieRuntimeLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(kieSession,
                    kieRuntimeLoggerFile.absolutePath)
        }

        kieSession.setGlobal('log', log)
        kieSession.setGlobal('engine', facade)
        kieSession.setGlobal('stringify', Helpers.stringify(this))
    }

    @SuppressWarnings(['MethodParameterTypeRequired', 'NoDef'])
    @PackageScope
    void addOrReplaceKieObject(KieSession kieSession, object, String[] changed = null) {
        if (kieSession == null) {
            kieSession = this.kieSession
        }
        Objects.requireNonNull(kieSession, 'KIE session not initialized')
        UUID id = object.id
        if (id) {
            FactHandle handle = handles.get(id)
            if (handle) {
                if (object instanceof KieMutableProperties) {
                    String[] props = ((KieMutableProperties) object).kieMutableProperties()
                    if (props.length > 0) { // empty props indicates immutable
                        def existing = kieSession.getObject(handle)
                        if (changed == null) {
                            changed = (existing == null || existing.is(object)) ?
                                    props :
                                    props.findAll { existing.getProperty(it) != object.getProperty(it) } as String[]
                        }
                        if (changed.length > 0) {
                            log.debug 'Object {} changed props are {}', object, changed
                            kieSession.update(handle, object, changed)
                        }
                    }
                } else {
                    kieSession.update(handle, object)
                }
            } else {
                handles.put(id, kieSession.insert(object))
            }
        } else {
            throw new IllegalStateException("Transient object ${object} requested to add to the KIE session")
        }
    }

    private void addOrReplaceKieObjects(KieSession kieSession, Object... objects) {
        objects.flatten().each { addOrReplaceKieObject(kieSession, it) }
    }

    @SuppressWarnings(['MethodParameterTypeRequired', 'NoDef'])
    private void removeKieObject(KieSession kieSession, object) {
        if (kieSession == null) {
            return
        }
        UUID id = object.id
        if (id) {
            FactHandle handle = handles.get(id)
            if (handle) {
                kieSession.delete(handle)
                handles.remove(id)
            } else {
                throw new IllegalStateException(
                        "Object ${object} requested to remove from the KIE session but not FactHandle found")
            }
        } else {
            throw new IllegalStateException("Transient object ${object} requested to remove from the KIE session")
        }
    }

    private void removeKieObjects(KieSession kieSession, Object... objects) {
        if (kieSession == null) {
            return
        }
        objects.flatten().each { removeKieObject(kieSession, it) }
    }

    /**
     * Initialize the facts in the KIE session from the story.
     */
    private void initKieSession() {
        if (!initKieSessionLatch.compareAndSet(false, true)) {
            return
        }
        chronosHandle = kieSession.insert(story.chronos)
        storyStateHandle = kieSession.insert(new StoryState(story))
        populateKieSession(kieSession)
    }

    private void populateKieSession(KieSession kieSession) {
        syncStoryState(kieSession)
        addOrReplaceKieObjects(kieSession, story.world.rooms, story.goals, story.cast, story.requests)
        populateKieSessionHistory(kieSession, story.history.events)
    }

    private void populateKieSessionHistory(KieSession kieSession, Collection<Event> events) {
        addOrReplaceKieObjects(kieSession, events)
        addOrReplaceKieObjects(kieSession, events*.players)
    }

    private void doIgnore(PlayerRequest request) {
        if (story.requests.remove(request)) {
            Objects.requireNonNull(request.id)
            publisher.submit(new RequestSatisfied(request))
        }
    }

    private void doAddToCast(Player player) {
        boolean removed = false
        Iterator<Request> iter = story.requests.iterator()
        while (iter.hasNext()) {
            Request r = iter.next()
            if (!(r instanceof PlayerRequest)) {
                continue
            }
            PlayerRequest pr = r as PlayerRequest
            if (pr.template.persona.name == player.persona.name) {
                iter.remove()
                player.siblingNumber = story.cast.count { it.persona.name == player.persona.name } + 1
                removed = true
                Objects.requireNonNull(pr.id)
                publisher.submit(new RequestSatisfied(pr))
                break
            }
        }
        if (!removed) {
            throw new IllegalArgumentException("Cannot find template matching player ${player}")
        }
        story.cast.add(player)
        Objects.requireNonNull(player.id)
        publisher.submit(new PlayerChanged(player.cloneKeepId(), 0))
    }

    private void startRuleEngine(CompletableFuture<Void> future) {
        if (!autoLifecycle) {
            future.complete(null)
            return
        }
        if (!firingLatch.compareAndSet(false, true)) {
            future.complete(null)
            return
        }
        firingComplete = new CompletableFuture<>()
        executor.execute {
            boolean success = false
            try {
                int quickRestartCount = 0
                future.complete(null)
                while (!halting.get() && quickRestartCount < 3) {
                    log.info 'Rule engine firing until halt'
                    long start = System.currentTimeMillis()
                    final KieSession kieSession1 = kieSession
                    if (kieSession1 == null) {
                        break
                    }
                    kieSession1.fireUntilHalt()
                    long end = System.currentTimeMillis()
                    if (halting.get()) {
                        log.info 'Halted rule engine'
                    } else {
                        // this can happen if a thread is not done calling .fireAllRules()
                        log.debug 'Rule engine stopped without call to halt()'
                    }
                    if ((end - start) < 10000) {
                        quickRestartCount++
                    } else {
                        quickRestartCount = 0
                    }
                }
                if (!halting.get()) {
                    log.error 'Rule engine repeatedly stopping without call to halt()'
                }
                success = true
            } catch (Exception e) {
                firingComplete.completeExceptionally(e)
            } finally {
                firingComplete.complete(success)
                firingLatch.set(false)
            }
        }
    }

    private void placePlayers() {
        Set<UUID> appliedTemplates = story.cast.collect { it.template?.id }.unique() as Set
        story.world.extras.findAll { !(it.id in appliedTemplates) }.each { PlayerTemplate t ->
            Collection<Player> players = t.createPlayers()
            story.cast.addAll(players)
            players.each {
                Objects.requireNonNull(it.id)
                publisher.submit(new PlayerChanged(it.cloneKeepId(), 0))
            }
        }
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    private void createPlayerRequests() {
        story.world.players.each { PlayerTemplate template ->
            int i = 0
            while (i < template.quantity.to) {
                PlayerRequest playerRequest = new PlayerRequest(template, i >= template.quantity.from)
                playerRequest.story = story
                story.requests << playerRequest
                i++
                Objects.requireNonNull(playerRequest.id)
                publisher.submit(new RequestCreated(playerRequest))
            }
        }
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    private void createGoalStatus() {
        story.world.goals.each { Goal goal ->
            GoalStatus status = new GoalStatus(story: story, goal: goal, fulfilled: false)
            story.goals << status
        }
    }

    private void syncStoryState(KieSession kieSession) {
        StoryState state = kieSession.getObject(storyStateHandle) as StoryState
        if (state != null) {
            state.update(story)
            kieSession.update(storyStateHandle, state)
        } else {
            log.warn 'Unable to sync story state, no object found with handle {}', storyStateHandle
        }
    }

    private void checkInitComplete(final CompletableFuture<Void> future) {
        if (autoLifecycle && story.requests.empty) {
            start().thenApply { s -> future.complete(s) }
        } else {
            future.complete(null)
        }
    }

    protected boolean isFiring() {
        if (kieSession == null) {
            return false
        }
        ((InternalAgenda) kieSession.agenda).isFiring()
    }

    protected void incrementChronos() {
        log.debug('Incrementing chronos')
        if (chronosLimit && story.chronos.current >= chronosLimit) {
            throw new IllegalStateException("Chronos exceeded limit of ${chronosLimit}")
        }
        Event event = recordHistory()
        story.chronos.next()
        kieSession.update(chronosHandle, story.chronos, 'current')
        populateKieSessionHistory(kieSession, [event])
        publisher.submit(new ChronosChanged(story.chronos.current))
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    protected void createActionRequest(Player player) {
        ActionRequest request = new ActionRequest(
                player,
                story.chronos.current,
                story.roomSummary(player.room, player, bundles))
        request.story = story
        if (!story.requests.contains(request)) {
            log.debug('Creating action request {}', request)
            request.actions.addAll(actionStatementParser.findAvailableActions(facade, player))
            request.directions.addAll(player.room.neighbors.keySet().sort())
            story.requests.add(request)
            addOrReplaceKieObject(kieSession, request)
            publisher.submit(new RequestCreated(request))
        }
    }

    /**
     * Create a new event in history.
     * @return the new event
     */
    private Event recordHistory() {
        Event event = new Event(story.chronos)
        event.players.addAll(story.cast.collect {
            new PlayerEvent(event: event, player: it.clone(), action: currentActions.get(it.id)) })
        story.history.addEvent(event)
        currentActions.clear()
        event
    }

    /**
     * See {@link KieSession#getQueryResults(java.lang.String, java.lang.Object...)}.
     */
    private QueryResults queryRuleEngine(String queryName, Object... args) {
        Objects.requireNonNull(kieSession)
        kieSession.getQueryResults(queryName, args)
    }
}
