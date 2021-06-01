package cucumber.steps.org.patdouble.adventuregame

import io.cucumber.groovy.EN
import org.patdouble.adventuregame.pages.HomePage

this.metaClass.mixin(EN)

When(~/^Home Page is visited$/) { ->
    to HomePage
}

When(~/^New story for "([^"]+)" is created$/) { String worldName ->
    to HomePage
    waitFor('slow') {
        page.stories.find { it.title == worldName }.startButton.click()
    }
}

When(~/^"([^"]+)" is chosen as a player with full name "([^"]+)" and nick name "([^"]+)"$/) {
    String playerName, String fullName, String nickName ->

}

When(~/^Story is started$/) {

}
