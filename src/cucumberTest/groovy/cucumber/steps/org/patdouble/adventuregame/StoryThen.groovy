package cucumber.steps.org.patdouble.adventuregame

import io.cucumber.groovy.EN
import org.patdouble.adventuregame.pages.HomePage

this.metaClass.mixin(EN)

Then(~/^World "([^"]+)" is listed$/) { String worldName ->
    at HomePage
    waitFor('slow') {
        page.stories.find { it.title == worldName }
    }
}

Then(~/^"([^"]+)" is available as a player$/) { String playerName ->

}

Then(~/^Story may be started$/) {

}

Then(~/^"([^"]+)" has a turn$/) { String playerName ->

}
