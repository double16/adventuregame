package org.patdouble.adventuregame.engine

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.kie.api.KieServices
import org.kie.api.runtime.KieContainer
import org.kie.api.runtime.KieSession
import org.kie.api.runtime.KieSessionConfiguration
import org.kie.api.runtime.rule.FactHandle
import org.kie.internal.runtime.conf.ForceEagerActivationOption
import org.patdouble.adventuregame.flow.ChronosChanged
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

    final Story story
    final Locale locale = Locale.ENGLISH
    final ActionStatementParser actionStatementParser
    final Bundles bundles
    /** Automatically move through the lifecycle. */
    boolean autoLifecycle
    /** Stops the engine if the chronos reaches this value, prevents run away execution. */
    Integer chronosLimit

    private final Executor executor
    @Delegate(includeTypes = [Flow.Publisher])
    final private SubmissionPublisher<StoryMessage> publisher

    DroolsConfiguration droolsConfiguration
    KieContainer kContainer
    private KieSession kieSession
    private FactHandle chronosHandle

    Engine(Story story, Executor executor = null) {
        this.story = story
        this.executor = executor
        if (this.executor == null) {
            this.executor = ForkJoinPool.commonPool()
        }
        this.publisher = new SubmissionPublisher<>(this.executor, Flow.defaultBufferSize())
        actionStatementParser = new ActionStatementParser(locale)
        bundles = Bundles.get(locale)
        droolsConfiguration = new DroolsConfiguration()
    }

    /**
     * Initialize the story to be ready to play, which includes creating request for players.
     */
    void init() {
        assert story.world
        story.chronos = new Chronos()
        story.history = new History(story.world)

        createPlayerRequests()
        createGoalStatus()

        // let caller create KieContainer if desired
        if (!kContainer) {
            WorldRuleGenerator worldRuleGenerator = new WorldRuleGenerator(story.world)
            StringWriter drl = new StringWriter()
            StringWriter dslr = new StringWriter()
            worldRuleGenerator.generate(drl, dslr)
            kContainer = droolsConfiguration.kieContainer(drl.toString(), dslr.toString())
        }
        KieSessionConfiguration ksConfig = KieServices.Factory.get().newKieSessionConfiguration()
        ksConfig.setOption(ForceEagerActivationOption.YES)
        kieSession = kContainer.newKieSession(ksConfig)
        initKieSession()
        if (autoLifecycle) {
            Thread kieThread = new Thread(KIE_THREAD_GROUP, "kie for ${story.world.name}") {
                @Override
                void run() {
                    kieSession.fireUntilHalt()
                    kieSession.dispose()
                }
            }
            kieThread.daemon = true
            kieThread.start()
        }
    }

    /**
     * Initialize the facts in the KIE session from the story.
     */
    private void initKieSession() {
        kieSession.setGlobal('log', log)
        kieSession.setGlobal('engine', new EngineFacade(this))
        chronosHandle = kieSession.insert(story.chronos)
        story.world.rooms.each { kieSession.insert(it) }
        story.cast.each { kieSession.insert(it) }
        story.requests.each { kieSession.insert(it) }
        story.goals.each { kieSession.insert(it) }
    }

    /**
     * Adds a player to satisfy a {@link PlayerRequest}.
     * @throw IllegalArgumentException if a matching PlayerRequest isn't found.
     */
    @SuppressWarnings('Instanceof')
    void addToCast(Player player) {
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
                removed = true
                kieSession.delete(kieSession.getFactHandle(r))
                publisher.submit(new RequestSatisfied(pr))
                break
            }
        }
        if (!removed) {
            throw new IllegalArgumentException("Cannot find template matching player ${player}")
        }
        story.cast << player
        kieSession.insert(player)
        publisher.submit(new PlayerChanged(player.clone(), 0))
        checkInitComplete()
    }

    /**
     * Ignore the request.
     * @throws IllegalArgumentException if the request isn't optional
     */
    void ignore(PlayerRequest request) {
        if (!request.optional) {
            throw new IllegalArgumentException("Can not ignore required player ${request.template}")
        }
        kieSession.delete(kieSession.getFactHandle(request))
        story.requests.remove(request)
        checkInitComplete()
    }

    private void checkInitComplete() {
        if (autoLifecycle && story.requests.empty) {
            start()
        }
    }

    /**
     * Start the game after the required cast members have been satisfied. Any requests for optional p[layers will be
     * removed from the story by this method.
     * @throew IllegalStateException if there are required players not yet cast
     */
    @SuppressWarnings('Instanceof')
    void start() {
        Collection<PlayerRequest> pendingPlayers = story.requests
                .findAll { it instanceof PlayerRequest && !it.optional }
        if (!pendingPlayers.empty) {
            throw new IllegalStateException("Required players: ${pendingPlayers*.template*.fullName}")
        }
        story.requests.findAll { it instanceof PlayerRequest }
                .collect { kieSession.getFactHandle(it) }
                .findAll()
                .each { kieSession.delete(it) }
        story.requests.removeIf { it instanceof PlayerRequest }

        placePlayers()
        createGoalStatus()
        next()
    }

    private void placePlayers() {
        story.world.extras.each { ExtrasTemplate t ->
            Collection<Player> players = t.createPlayers()
            story.cast.addAll(players)
            players.each { kieSession.insert(it) }
            players.each { publisher.submit(new PlayerChanged(it.clone(), 0)) }
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

    void next() {
        kieSession.fireAllRules()
    }

    /**
     * End the story.
     */
    void end() {
        story.ended = true
        story.requests.each {
            kieSession.delete(kieSession.getFactHandle(it))
        }
        story.requests.clear()
        publisher.submit(new StoryEnded())
    }

    /**
     * Close and cleanup the engine. This does not end the story.
     */
    @Override
    void close() throws IOException {
        if (!publisher.isClosed()) {
            publisher.close()
        }
        kieSession.halt()
        kieSession.dispose()
    }

    /**
     * Check if this engine is closed, i.e. it is no longer running a story. This does not indicate the story has
     * ended.
     */
    boolean isClosed() {
        publisher.isClosed()
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone()
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
            request.actions = actionStatementParser.availableActions.asImmutable()
            request.directions = player.room.neighbors.keySet().sort().asImmutable()
            story.requests.add(request)
            kieSession.insert(request)
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

    /**
     * Resend requests as RequestCreated messages.
     */
    void resendRequests() {
        story.requests.each { publisher.submit(new RequestCreated(it)) }
    }

    /**
     * Request the player performs an action.
     * @return true if the action was successful, false otherwise. Any error message will be sent via flow.
     */
    boolean action(Player player, String statement) {
        action(player, (ActionStatement) actionStatementParser.parse(statement))
    }

    /**
     * Request the player performs an action.
     * @return true if the action was successful, false otherwise. Any error message will be sent via flow.
     */
    @SuppressWarnings('Instanceof')
    boolean action(Player player, ActionStatement action) {
        log.info('action for player {} - {}', player, action)
        ActionRequest actionRequest = story.requests.find { it instanceof ActionRequest && it.player == player }
        if (player.motivator == Motivator.HUMAN) {
            if (!actionRequest) {
                log.error('action for player {} without request, action = {}', player, action)
                publisher.submit(new PlayerNotification(player,
                        bundles.text.getString('action.norequest.subject'),
                        bundles.text.getString('action.norequest.text')))
                return false
            }
        }

        boolean success = false
        boolean validAction = false

        Action builtInAction = action?.getVerbAsAction()
        if (builtInAction) {
            switch (builtInAction) {
                case Action.GO:
                    validAction = true
                    success = actionGo(player, action)
                    break
                case Action.WAIT:
                    validAction = true
                    success = actionWait()
                    break
            }
        } else if (action?.verb) {
            // TODO: custom actions
            log.warn('custom actions not yet supported: {}', action.verb)
        }

        if (!validAction) {
            publisher.submit(new PlayerNotification(player,
                    bundles.text.getString('action.invalid.subject'),
                    bundles.actionInvalidTextTemplate.make([ actions: actionStatementParser.availableActions ])
                            .toString()))
            if (actionRequest) {
                publisher.submit(new RequestCreated(actionRequest))
            }
        } else if (success) {
            kieSession.submit { kieSession ->
                player.chronos = story.chronos.current
                kieSession.update(kieSession.getFactHandle(player), player)
                if (actionRequest) {
                    story.requests.remove(actionRequest)
                    kieSession.delete(kieSession.getFactHandle(actionRequest))
                    publisher.submit(new RequestSatisfied(actionRequest))
                }
            }
        } else {
            if (actionRequest) {
                publisher.submit(new RequestCreated(actionRequest))
            }
        }
        next()
        success
    }

    private boolean actionGo(Player player, ActionStatement action) {
        String direction = action.directObject?.toLowerCase()
        List<String> directions = player.room.neighbors.keySet().sort()

        // match direction
        List<String> candidates
        if (!direction) {
            candidates = Collections.emptyList()
        } else if (directions.contains(direction)) {
            candidates = Collections.singletonList(direction)
        } else {
            candidates = directions.findAll { it.startsWith(direction) }
        }

        if (candidates.size() != 1) {
            publisher.submit(new PlayerNotification(player,
                    bundles.text.getString('action.go.instructions.subject'),
                    bundles.goInstructionsTemplate.make([ directions: directions ]).toString()))
            return false
        }

        player.room = player.room.neighbors.get(candidates.first())
        publisher.submit(new PlayerChanged(player.clone(), story.chronos.current))

        true
    }

    private boolean actionWait() {
        true
    }
}
