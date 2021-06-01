package cucumber.steps.org.patdouble.adventuregame

import io.cucumber.groovy.EN

this.metaClass.mixin(EN)

When(~/^Screenshot is taken$/) { ->
    screenshot()
}

When(~/^Cookies are dumped$/) { ->
    scenario.log("COOKIES: " + browser.driver?.manage()?.cookies?.toString())
}
