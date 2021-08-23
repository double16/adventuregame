package cucumber.steps.org.patdouble.adventuregame

import io.cucumber.groovy.EN
import org.patdouble.adventuregame.pages.HomePage
import org.patdouble.adventuregame.pages.StoryInitPage
import org.patdouble.adventuregame.pages.StoryRunPage

this.metaClass.mixin(EN)

Then(~/^World "([^"]+)" is listed$/) { String worldName ->
    at HomePage
    waitFor('slow') {
        page.stories.find { it.title == worldName }
    }
}

Then(~/^"([^"]+)" is available as a player$/) { String playerName ->
    at StoryInitPage
    waitFor('slow') {
        page.players.find { it.title.startsWith(playerName) }
    }
}

Then(~/^Story may be started$/) { ->
    at StoryInitPage
    waitFor('slow') {
        page.startStory
    }
}

Then(~/^"([^"]+)" has a turn$/) { String playerName ->
    at StoryRunPage
    page.currentPlayerFullName.startsWith(playerName)
    waitFor('slow') {
        page.actionRequest.submit
    }
}

Then(~/^Notification has "([^"]+)"$/) { String message ->
    at StoryRunPage
    waitFor('slow') {
        page.notifications.find { it.message.contains(message) }
    }
}

Then(~/^Room is "([^"]+)"$/) { String roomName ->
    at StoryRunPage
    waitFor('slow') {
        page.actionRequest.roomName == roomName
    }
}
