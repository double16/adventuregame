package org.patdouble.adventuregame.state

import org.patdouble.adventuregame.storage.yaml.YamlUniverseRegistry
import spock.lang.Specification

class StoryTest extends Specification {
    YamlUniverseRegistry registry = new YamlUniverseRegistry()

    private Story newStory(String worldName) {
        new Story(registry.worlds.find { it.name == worldName })
    }

    def "Equals"() {
        expect:
        newStory(YamlUniverseRegistry.TRAILER_PARK) == newStory(YamlUniverseRegistry.TRAILER_PARK)
        newStory(YamlUniverseRegistry.TRAILER_PARK) != newStory(YamlUniverseRegistry.THE_HOBBIT)
    }

    def "Hashcode"() {
        expect:
        newStory(YamlUniverseRegistry.TRAILER_PARK).hashCode() == newStory(YamlUniverseRegistry.TRAILER_PARK).hashCode()
        newStory(YamlUniverseRegistry.TRAILER_PARK).hashCode() != newStory(YamlUniverseRegistry.THE_HOBBIT).hashCode()
    }
}
