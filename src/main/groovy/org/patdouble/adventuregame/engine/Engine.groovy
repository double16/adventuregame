package org.patdouble.adventuregame.engine

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.apache.maven.lifecycle.MissingProjectException
import org.drools.core.common.InternalAgenda
import org.kie.api.KieServices
import org.kie.api.runtime.KieContainer
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.KieSessionConfiguration
import org.kie.api.runtime.rule.FactHandle
import org.kie.internal.runtime.conf.ForceEagerActivationOption
import org.patdouble.adventuregame.flow.ChronosChanged
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
import org.patdouble.adventuregame.model.ExtrasTemplate
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.state.Chronos
import org.patdouble.adventuregame.state.Event
import org.patdouble.adventuregame.state.GoalStatus
import org.patdouble.adventuregame.state.History
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.Story
import org.patdouble.adventuregame.state.request.ActionRequest
import org.patdouble.adventuregame.state.request.PlayerRequest
import org.patdouble.adventuregame.state.request.Request

import javax.validation.constraints.NotNull
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Flow
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.SubmissionPublisher

/**
 * Runs the story by advancing the {@link Chronos}, declaring necessary inputs from humans, meeting goals for AI
 * players, etc.
 * Responsible for updating the history.
 *
 * The entire state is stored in the {@link Story} object.
 */
@Slf4j
@CompileDynamic
class Engine implements Closeable {
    @SuppressWarnings('ThreadGroup')
    final static ThreadGroup KIE_THREAD_GROUP = new ThreadGroup('kie-sessions')

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
    @Delegate(includeTypes = [Flow.Publisher])
    final private SubmissionPublisher<StoryMessage> publisher

    private DroolsConfiguration droolsConfiguration
    private KieContainer kContainer
    private KieSession kieSession
    private FactHandle chronosHandle
    private FactHandle storyStateHandle
    private final Map<UUID, FactHandle> handles = [:]

    Engine(@NotNull Story story, Executor executor = null) {
        Objects.requireNonNull(story)
        this.facade = new EngineFacade(this)
        this.story = story
        this.executor = executor ?: ForkJoinPool.commonPool()
        this.publisher = new SubmissionPublisher<>(this.executor, Flow.defaultBufferSize())
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
        kieSession.submit {
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
    CompletableFuture<Story> updateStory(Story story) {
        Objects.requireNonNull(story)
        final CompletableFuture<Story> future = new CompletableFuture<>()
        kieSession.submit {
            if (story.is(this.story)) {
                future.complete(story)
            } else {
                this.story = story
                if (storyStateHandle != null) {
                    populateKieSession()
                    syncStoryState()
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
        kieSession.submit {
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
        if (!request.optional) {
            future.completeExceptionally(new IllegalArgumentException("Can not ignore required player ${request.template}"))
        } else {
            kieSession.submit {
                try {
                    doIgnore(request)
                    checkInitComplete(future)
                } catch (RuntimeException e) {
                    future.completeExceptionally(e)
                }
            }
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
        kieSession.submit {
            try {
                if (forceMotivator == null) {
                    Collection<PlayerRequest> pendingPlayers = story.requests
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

                if (story.chronos.current == 0) {
                    placePlayers()
                    startRuleEngine()
                }

                future.complete(null)
            } catch (RuntimeException e) {
                future.completeExceptionally(e)
            }
        }
        future.thenRun { next() }
    }

    /**
     * Progress the story. Only necessary when {@link #autoLifecycle} is false. Otherwise this is a NOP.
     */
    void next() {
        kieSession.fireAllRules()
    }

    /**
     * End the story.
     */
    CompletableFuture<Void> end() {
        if (kieSession == null) {
            return CompletableFuture.completedFuture(null)
        }

        final CompletableFuture<Void> future = new CompletableFuture<>()
        kieSession.submit {
            if (!story.ended) {
                story.ended = true
                syncStoryState()
                removeKieObjects(story.requests)
                story.requests.clear()
                publisher.submit(new StoryEnded())
            }
            future.complete(null)
        }
        future
    }

    /**
     * Close and cleanup the engine. This does not end the story.
     */
    @Override
    void close() throws IOException {
        if (!publisher.isClosed()) {
            publisher.close()
        }
        if (kieSession) {
            kieSession.halt()
            kieSession.dispose()
            kieSession = null
        }
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
        log.info('action for player {} - {}', player, action)

        final CompletableFuture<Boolean> future = new CompletableFuture<>()

        kieSession.submit {
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

                Action builtInAction = action?.getVerbAsAction()
                if (builtInAction) {
                    ActionExecutor actionInstance = builtInAction.actionClass.getConstructor().newInstance()
                    try {
                        success = actionInstance.execute(facade, player, action)
                        validAction = true
                    } catch (UnsupportedOperationException e) {
                        validAction = false
                    }
                } else if (action?.verb) {
                    // TODO: custom actions
                    log.warn('custom actions not yet supported: {}', action.verb)
                }

                if (!validAction) {
                    Objects.requireNonNull(player.id)
                    publisher.submit(new PlayerNotification(player,
                            bundles.text.getString('action.invalid.subject'),
                            bundles.actionInvalidTextTemplate.make([actions: actionStatementParser.availableActions])
                                    .toString()))
                    if (actionRequest) {
                        Objects.requireNonNull(actionRequest.id)
                        publisher.submit(new RequestCreated(actionRequest))
                    }
                } else if (success) {
                    player.chronos = story.chronos.current
                    kieSession.update(handles.get(player.id), player)
                    Objects.requireNonNull(player.id)
                    publisher.submit(new PlayerChanged(player.cloneKeepId(), story.chronos.current))

                    if (actionRequest) {
                        story.requests.remove(actionRequest)
                        removeKieObject(actionRequest)
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
            next()
            return s
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

        WorldRuleGenerator worldRuleGenerator = new WorldRuleGenerator(story.world)
        StringWriter drl = new StringWriter(16384)
        StringWriter dslr = new StringWriter(16384)
        worldRuleGenerator.generate(drl, dslr)
        kContainer = droolsConfiguration.kieContainer(drl.toString(), dslr.toString())

        KieSessionConfiguration ksConfig = KieServices.Factory.get().newKieSessionConfiguration()
        ksConfig.setOption(ForceEagerActivationOption.YES)
        kieSession = kContainer.newKieSession(ksConfig)
        kieSession.setGlobal('log', log)
        kieSession.setGlobal('engine', facade)
    }

    @SuppressWarnings(['MethodParameterTypeRequired', 'NoDef'])
    private void addOrReplaceKieObject(object) {
        Objects.requireNonNull(kieSession, 'KIE session not initialized')
        try {
            UUID id = object.id
            if (id) {
                FactHandle handle = handles.get(id)
                if (handle) {
                    kieSession.update(handle, object)
                } else {
                    handles.put(id, kieSession.insert(object))
                }
            } else {
                throw new IllegalStateException("Transient object ${object} requested to add to the KIE session")
            }
        } catch (MissingProjectException e) {
            throw new IllegalStateException("Object ${object} requested to add to the KIE session but has no 'id' property", e)
        }
    }

    private void addOrReplaceKieObjects(Object... objects) {
        objects.flatten().each { addOrReplaceKieObject(it) }
    }

    @SuppressWarnings(['MethodParameterTypeRequired', 'NoDef'])
    private void removeKieObject(object) {
        if (kieSession == null) {
            return
        }
        try {
            UUID id = object.id
            if (id) {
                FactHandle handle = handles.get(id)
                if (handle) {
                    kieSession.delete(handle)
                    handles.remove(id)
                } else {
                    throw new IllegalStateException("Object ${object} requested to remove from the KIE session but not FactHandle found")
                }
            } else {
                throw new IllegalStateException("Transient object ${object} requested to remove from the KIE session")
            }
        } catch (MissingPropertyException e) {
            throw new IllegalStateException("Object ${object} requested to remove from the KIE session but has no 'id' property", e)
        }
    }

    private void removeKieObjects(Object... objects) {
        if (kieSession == null) {
            return
        }
        objects.flatten().each { removeKieObject(it) }
    }

    /**
     * Initialize the facts in the KIE session from the story.
     */
    private void initKieSession() {
        chronosHandle = kieSession.insert(story.chronos)
        storyStateHandle = kieSession.insert(new StoryState(story))
        populateKieSession()
    }

    private void populateKieSession() {
        syncStoryState()
        addOrReplaceKieObjects(story.world.rooms, story.cast, story.requests, story.goals)
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

    private void startRuleEngine() {
        initKieSession()
        if (autoLifecycle) {
            Thread kieThread = new Thread(KIE_THREAD_GROUP, "kie for ${story.world.name}") {
                @Override
                void run() {
                    kieSession.fireUntilHalt()
                    kieSession?.dispose()
                }
            }
            kieThread.daemon = true
            kieThread.start()
        }
    }

    private void placePlayers() {
        story.world.extras.each { ExtrasTemplate t ->
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
            GoalStatus status = new GoalStatus(goal: goal, fulfilled: false)
            story.goals << status
        }
    }

    private void syncStoryState() {
        ((StoryState) kieSession.getObject(storyStateHandle))?.update(story)
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
        log.info('Incrementing chronos')
        if (chronosLimit && story.chronos.current >= chronosLimit) {
            throw new IllegalStateException("Chronos exceeded limit of ${chronosLimit}")
        }
        recordHistory()
        story.chronos++
        kieSession.update(chronosHandle, story.chronos)
        publisher.submit(new ChronosChanged(story.chronos.current))
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    protected void createActionRequest(Player player) {
        ActionRequest request = new ActionRequest(
                player,
                story.chronos.current,
                story.roomSummary(player.room, player, bundles))
        if (!story.requests.contains(request)) {
            log.info('Creating action request {}', request)
            request.actions.addAll(actionStatementParser.availableActions)
            request.directions.addAll(player.room.neighbors.keySet().sort())
            story.requests.add(request)
            addOrReplaceKieObject(request)
            publisher.submit(new RequestCreated(request))
        }
    }

    /**
     * Create a new event in history.
     * @return the new event
     */
    private Event recordHistory() {
        null
    }
}
