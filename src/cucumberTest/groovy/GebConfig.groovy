import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

/*
  Parameters:
  In browser caps (can be set for local browser using system property 'geb.browser.<key>'):
  - width
  - height
*/

waiting {
    timeout = 10
    retryInterval = 0.5
    presets {
        compute {
            timeout = 500
            retryInterval = 20
        }
        network {
            timeout = 300
            retryInterval = 20
        }
        slow {
            timeout = 20
            retryInterval = 1
        }
        quick {
            timeout = 5
        }
    }
}

atCheckWaiting = true
baseNavigatorWaiting = true

Closure<Map<String, Object>> collectBrowserCapsFromSystemProperties = {
    System.properties
            .findAll { String key, String value -> key.startsWith('geb.browser.')}
            .collectEntries { String key, String value -> [ key.substring(12), value ]}
}

Map<String, Object> augmentBrowserCaps(Map<String, Object> browserCaps) {
    def caps = [:] as Map<String, Object>
    caps << browserCaps
    caps.put('name', 'adventuregame')
    caps.put('build', 'git rev-parse HEAD'.execute().text.trim())
    caps.put('tags', [
            System.getenv('CI_BRANCH') ?: 'git symbolic-ref -q --short HEAD'.execute().text.trim(),
            'git status --porcelain'.execute().text ? 'dev' : ''
    ].findAll())
    caps.put('selenium-version', System.properties.'selenium.version')
    caps.put('elementScrollBehavior', 1) // necessary to avoid issues with the hovering top navbar

    if (System.getProperty('geb.build.initialUrl')) {
        caps.put('initialBrowserUrl', System.getProperty('geb.build.initialUrl'))
    } else if (System.getProperty('geb.build.baseUrl')) {
        caps.put('initialBrowserUrl', System.getProperty('geb.build.baseUrl'))
    }

    caps
}

def postProcessDriver = { WebDriver driver, Map caps = augmentBrowserCaps(collectBrowserCapsFromSystemProperties.call()) ->
    int width = 1280, height = 1024
    if (caps['width'] && caps['height']) {
        width = caps['width'] as int
        height = caps['height'] as int
    }
    if (width > 0 && height > 0) {
        driver.manage().window().setSize(new Dimension(width, height))
    }

    driver
}

WebDriverManager.chromedriver().setup()
driver = {
    try {
        WebDriverManager.chromedriver().setup()
        ChromeOptions opts = new ChromeOptions()
        opts.addArguments('headless')
        opts.addArguments('dns-prefetch-disable')
        postProcessDriver.call(new ChromeDriver(opts))
    } catch (Throwable e) {
        new File('chromedriver.log').withOutputStream {
            e.printStackTrace(new PrintStream(it))
        }
        throw e
    }
}
