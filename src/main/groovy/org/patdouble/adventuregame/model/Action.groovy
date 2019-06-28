package org.patdouble.adventuregame.model

/**
 * Recommended actions. Any verb can be used but these may come with built in synonyms.
 */
enum Action {
    GO('move'),
    PAY,
    SWIM,
    FIGHT,
    FLEE;

    List<String> synonyms

    Action(String ... synonyms) {
        this.synonyms = Collections.unmodifiableList(Arrays.asList(synonyms))
    }
}
