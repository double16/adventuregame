package org.patdouble.adventuregame.i18n

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.patdouble.adventuregame.engine.EngineFacade
import org.patdouble.adventuregame.model.Action
import org.patdouble.adventuregame.state.Player

import javax.validation.constraints.NotNull
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Parses a statement intended to describe a player's action.
 *
 * https://www.butte.edu/departments/cas/tipsheets/grammar/parts_of_speech.html
 */
@Slf4j
@CompileDynamic
class ActionStatementParser {
    private static final String RE_OR = '|'
    private static final String PROP_VALUE_SEP = /,+/
    private final Locale locale
    private final Bundles bundles
    private final Pattern pattern
    private final Map<String, Action> actions = [:]

    ActionStatementParser(Locale locale = Locale.ENGLISH) {
        this.locale = locale
        bundles = Bundles.get(locale)

        Action.values().each { action ->
            bundles.actions.getString("action.${action.name().toLowerCase()}").split(PROP_VALUE_SEP).each {
                actions.put(it.trim().toLowerCase(), action)
            }
        }

        List<String> articles = bundles.actions.getString('articles').split(PROP_VALUE_SEP)*.trim()
        List<String> prepositions = bundles.actions.getString('prepositions').split(PROP_VALUE_SEP)*.trim()

        pattern = Pattern.compile(

/\s*
# action
(${actions.keySet().join(RE_OR).replaceAll(~/\s+/, /\\\s+/)})

# direct object
(?:
(?:\s+(?:${articles.join(RE_OR)}))?
\s+(\w(?:[\w\s]+?\w)?)

# prepositions - ... with ...
(?:
(?:\s+(?:${prepositions.join(RE_OR)}))
(?:\s+(?:${articles.join(RE_OR)}))?
\s+(\w[\w\s]+?\w)
)?
)?
\s*/
                , Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS | Pattern.COMMENTS)

//        log.debug "pattern is {}", pattern.pattern()
    }

    ActionStatement parse(@NotNull CharSequence nls) {
        Matcher matcher = pattern.matcher(nls ?: '')
        if (matcher.matches()) {
            String actionStr = matcher.group(1).toLowerCase().replaceAll(~/\s+/, ' ')
            if (actions.containsKey(actionStr)) {
                actionStr = actions.get(actionStr).name().toLowerCase()
            }
            return new ActionStatement(actionStr, matcher.group(2)?.toLowerCase(), matcher.group(3)?.toLowerCase())
        }
        null
    }

    List<String> findAvailableActions(EngineFacade facade, Player player) {
        actions.findAll { k, v -> v.actionClass.getDeclaredConstructor().newInstance().isValid(facade, player) }
                .keySet()
                .sort()
    }
}
