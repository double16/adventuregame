package cucumber.steps.org.patdouble.adventuregame

import io.cucumber.groovy.EN
import org.patdouble.adventuregame.pages.HomePage
import org.patdouble.adventuregame.pages.StoryInitPage
import org.patdouble.adventuregame.pages.StoryRunPage

this.metaClass.mixin(EN)

When(~/^Home Page is visited$/) { ->
    to HomePage
}

When(~/^New story for "([^"]+)" is created$/) { String worldName ->
    at HomePage
    waitFor('slow') {
        page.stories.find { it.title == worldName }.startButton.click()
    }
}

When(~/^"([^"]+)" is chosen as a player with full name "([^"]+)" and nick name "([^"]+)"$/) {
    String playerName, String fullName, String nickName ->
        at StoryInitPage
        waitFor('slow') {
            page.players.find { it.title.startsWith(playerName) }.select.click()
        }
        if (fullName) {
            page.fullName = fullName
        }
        if (nickName) {
            page.nickName = nickName
        }
        waitFor('slow') {
            page.playerSubmit.click()
        }
}

When(~/^Story is started$/) { ->
    at StoryInitPage
    waitFor('slow') {
        page.startStory.click()
    }
}

When(~/^Player action is "([^"]+)"$/) { String action ->
    at StoryRunPage
    page.actionRequest.statement = action
    waitFor('quick') {
        page.actionRequest.submit.click()
    }
}
