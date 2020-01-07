package org.patdouble.adventuregame.ui.console

import groovy.transform.CompileDynamic
import org.fusesource.jansi.Ansi
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import org.patdouble.adventuregame.engine.Engine
import org.patdouble.adventuregame.flow.PlayerNotification
import org.patdouble.adventuregame.flow.RequestCreated
import org.patdouble.adventuregame.flow.StoryMessage
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.request.ActionRequest
import org.patdouble.adventuregame.state.request.PlayerRequest

import java.util.concurrent.Flow

/**
 * All requests must be handled through a single subscriber because of the single use nature of the Console.
 */
@CompileDynamic
class ConsoleRequestHandler implements Flow.Subscriber<StoryMessage>, AutoCloseable {
    private Flow.Subscription subscription
    private final Console console
    private final Engine engine
    private final Set<PlayerTemplate> skippedPlayerTemplates = [] as Set

    ConsoleRequestHandler(Console console, Engine engine) {
        Objects.requireNonNull(console)
        Objects.requireNonNull(engine)
        this.console = console
        this.engine = engine
    }

    @Override
    void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription
        subscription.request(1)
    }

    @Override
    @SuppressWarnings('Instanceof')
    void onNext(StoryMessage item) {
        if (item instanceof RequestCreated) {
            switch (item['request'].class) {
                case PlayerRequest:
                    handle((PlayerRequest) item.request)
                    break
                case ActionRequest:
                    handle((ActionRequest) item.request)
                    break
                default:
                    break
            }
        } else if (item instanceof PlayerNotification) {
            handle((PlayerNotification) item)
        }

        subscription.request(1)
    }

    @Override
    @SuppressWarnings(['SystemExit', 'PrintStackTrace'])
    void onError(Throwable throwable) {
        throwable.printStackTrace()
        close()
        System.exit(2)
    }

    @Override
    @SuppressWarnings('SystemExit')
    void onComplete() {
        console.println {
            newline()
                .a(Ansi.Attribute.NEGATIVE_ON).a('--- GAME OVER ---').a(Ansi.Attribute.NEGATIVE_OFF)
                .newline()
                .reset() }
        console.close()
        System.exit(0)
    }

    @Override
    void close() throws Exception {
        subscription.cancel()
    }

    private void handle(PlayerRequest playerRequest) {
        if (skippedPlayerTemplates.contains(playerRequest.template)) {
            if (playerRequest.optional) {
                engine.addToCast(playerRequest.template.createPlayer(Motivator.AI))
            } else {
                engine.ignore(playerRequest)
            }
            return
        }

        try {
            console.println {
                newline()
                .a('Player ')
                if (playerRequest.template.nickName) {
                    bold().a(playerRequest.template.nickName).boldOff()
                            .a(' the ')
                }
                bold().a(playerRequest.template.persona.name).boldOff()
            }
            String includePlayer = console.readLine('Do you want to be this player (y/n) ? ').trim().toLowerCase()

            if (!includePlayer.startsWith('y')) {
                skippedPlayerTemplates.add(playerRequest.template)
                if (playerRequest.optional) {
                    engine.ignore(playerRequest)
                } else {
                    engine.addToCast(playerRequest.template.createPlayer(Motivator.AI))
                }
                return
            }

            String full = console.readLine('Full Name? ', playerRequest.template.fullName)
            String nick = console.readLine('Nick Name? ', playerRequest.template.nickName)
            Player player = playerRequest.template.createPlayer(Motivator.HUMAN)
            if (full) {
                player.fullName = full
            }
            if (nick) {
                player.nickName = nick
            }
            engine.addToCast(player)
        } catch (UserInterruptException | EndOfFileException e) {
            engine.close()
        }
    }

    private void handle(ActionRequest actionRequest) {
        console.println {
            newline()
            .bold().a(actionRequest.roomSummary.name).boldOff()
            .newline()
            .a(actionRequest.roomSummary.description)
        }

        if (actionRequest.roomSummary.occupants) {
            console.println {
                newline()
                .a(actionRequest.roomSummary.occupants)
            }
        }

        console.println()

        try {
            String command = console.readLine("${actionRequest.player} ? ")
            engine.action(actionRequest.player, command)
        } catch (UserInterruptException | EndOfFileException e) {
            engine.close()
        }
    }

    private void handle(PlayerNotification playerNotification) {
        console.println {
            newline()
            .bold().a(playerNotification.player.title).boldOff()
            .a(': ')
            .a(playerNotification.text) }
    }
}
