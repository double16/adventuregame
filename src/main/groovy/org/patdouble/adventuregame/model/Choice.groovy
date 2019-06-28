package org.patdouble.adventuregame.model

import org.patdouble.adventuregame.state.Player

class Choice {
    final String action
    final String result
    final int healthPenalty
    final BigDecimal wealthPenalty

    Choice(String action, String result, int healthPenalty, BigDecimal wealthPenalty) {
        this.action = action.toLowerCase()
        this.result = result
        this.healthPenalty = healthPenalty
        this.wealthPenalty = wealthPenalty
    }

    /**
     * Apply the choice made to the player. This will adjust the player's health, wealth, etc.
     *
     * @param player the player.
     * @return the story line for the choice.
     */
    String apply(Player player) {
        player.setHealth(player.getHealth() - healthPenalty)
        player.setWealth(player.getWealth().subtract(wealthPenalty))
        player.toString() + " choose to " + action + " and " + result
    }

    @Override
    String toString() {
        action
    }
}
