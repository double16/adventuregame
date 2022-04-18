Feature: Story can be created

  Scenario: Trailer Park Story is Created
    When Home Page is visited
    Then World "Trailer Park" is listed
    When New story for "Trailer Park" is created
    Then "Shadowblow the Hammer" is available as a player
    When "Shadowblow the Hammer" is chosen as a player with full name "Shadowblow the Hammer" and nick name "Shadowblow"
    Then Story may be started
    When Story is started
    Then "Shadowblow the Hammer" has a turn

  Scenario: Trailer Park Story is Played
    When Home Page is visited
    Then World "Trailer Park" is listed
    When New story for "Trailer Park" is created
    Then "Shadowblow the Hammer" is available as a player
    When "Shadowblow the Hammer" is chosen as a player with full name "Shadowblow the Hammer" and nick name "Shadowblow"
    Then Story may be started
    When Story is started
    Then "Shadowblow the Hammer" has a turn
    And  Notification has "Any player reaches trailer 2"
    When Player action is "go north"
    Then Room is "Trailer 2"