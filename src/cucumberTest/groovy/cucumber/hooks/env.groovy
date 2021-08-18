package cucumber.hooks

import geb.Browser
import geb.binding.BindingUpdater
import geb.driver.DriverCreationException
import io.cucumber.groovy.Hooks
import io.cucumber.groovy.Scenario
import org.apache.commons.text.StringEscapeUtils
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot

this.metaClass.mixin(Hooks)

void embedScreenshot(Scenario scenario) {
    try {
        if (browser.driver instanceof TakesScreenshot) {
            final byte[] screenshot = ((TakesScreenshot) browser.driver).getScreenshotAs(OutputType.BYTES)
            scenario.log("URL '${browser.getCurrentUrl()}'")
            scenario.attach(screenshot, 'image/png', 'screenshot.png')
        }
    } catch (UnsupportedOperationException e) {
        // we don't need to add a message for every call
    } catch (Exception e) {
        scenario.log("Could not embed screenshot: ${e.toString()}")
    }
}

void embedHTML(Scenario scenario) {
    try {
        String html = browser.driver.findElement(By.cssSelector("html")).getAttribute("innerHTML")
        scenario.attach(StringEscapeUtils.escapeHtml4(html).getBytes("UTF-8"), "text/html;charset=UTF-8", 'page.html')
    } catch (UnsupportedOperationException e) {
        // we don't need to add a message for every call
    } catch (Exception e) {
        scenario.log("Could not embed HTML: ${e.toString()}")
    }
}

void embedLogs(Scenario scenario) {
    try {
        String log = browser.driver.manage().logs().get("browser").collect { it.toString() }.join('\n')
        scenario.log("CONSOLE:\n${log}")
    } catch (UnsupportedOperationException e) {
        // we don't need to add a message for every call
    } catch (Exception e) {
        scenario.log("Could not embed logs: ${e.toString()}")
    }
}

class MyBindingUpdater extends BindingUpdater {
    MyBindingUpdater(Binding binding, Browser browser) {
        super(binding, browser)
    }
}

Before() { Scenario scenario ->
    try {
        scenario.log('[INFO] Creating the browser')

        Browser browser = new Browser()
        bindingUpdater = new MyBindingUpdater(binding, browser)
        bindingUpdater.initialize()

        binding.scenario = scenario

        binding.locale = Locale.ENGLISH
        binding.rnd = new Random()

        binding.screenshot = { embedScreenshot(scenario) }
    }
    catch (DriverCreationException e) {
        // the full stack trace doesn't get logged so we really don't know what happened
        e.cause?.printStackTrace()
        throw e
    }
}

After() { Scenario scenario ->
    scenario.log( "Local TimeZone is ${TimeZone.default.getID()}")
    scenario.log( "Locale is ${Locale.default}")

    if (scenario.isFailed()) {
        embedScreenshot(scenario)
        embedLogs(scenario)
        embedHTML(scenario)
    }

    if (binding.hasVariable('browser')) {
//        scenario.log('[INFO] Quitting the browser')
//        browser.quit()
        scenario.log('[INFO] Clearing cookies')
        browser.clearCookiesQuietly()
    }

    if (binding.hasVariable('bindingUpdater')) {
        scenario.log('[INFO] Removing Geb binding')
        bindingUpdater.remove()
    }
}
