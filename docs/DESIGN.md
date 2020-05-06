# adventuregame

## Chronos

The Chronos is a monotonically increasing number to mark time. It doesn't have an exact correspondence to clock time.

## Goals

A goal is a condition to be met defined by rules. The only limit of capability is the available rules. Goals can be required, which is usually only useful for AI players. Goals can also mark the end of the story, for good or bad.

World level goals can be used to end the story or trigger some other action in the world.

Player level goals are used by AI to direct the player's actions.

## Maps

Players will automatically remember where they've been. Each player will maintain a list of rooms and the chronos values when visited. Based on the *memory* value for the player, rooms will be forgotten based on age and frequency. Maps are generated using the DOT format and visualized with some form of the *Graphviz* software.

Human players will find the map useful by showing it via a command.

AI players use the map to meet goals, such as navigating to a room, player or item.

## Beacons

A goal may require an AI player to navigate to an item or player, either a specific item or player or based on attributes (virtue, etc.) In order to acquire the target, it will emit a beacon as it moves or interacts. The range of the beacon and length of time it persists is variable.

## AI Room Navigation

Room navigation can be done with solely mapping. With no information the AI will be exploring the map, seemingly randomly.

Persona attributes can be considered while exploring:
* leadership - a follower should follow the movements of other players
* virtue - would good follow evil, or vice versa?
* health, bravery - consider not going into risky rooms if low health or low on bravery
