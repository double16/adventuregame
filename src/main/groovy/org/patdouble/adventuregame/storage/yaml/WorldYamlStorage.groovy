package org.patdouble.adventuregame.storage.yaml

import groovy.transform.CompileDynamic
import org.patdouble.adventuregame.model.CharacterTrait
import org.patdouble.adventuregame.model.Direction
import org.patdouble.adventuregame.model.ExtrasTemplate
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World
import org.yaml.snakeyaml.Yaml

/**
 * Storage of World using YAML.
 */
@CompileDynamic
class WorldYamlStorage {
    public static final String KEY_NAME = 'name'
    public static final String KEY_DESCRIPTION = 'description'
    public static final String KEY_QUANTITY = 'quantity'
    public static final String KEY_ROOM = 'room'

    World load(InputStream is) throws IOException {
        Objects.requireNonNull(is)
        Map<String, Object> yaml
        try {
            Yaml parser = new Yaml()
            yaml = parser.load(is)
        } finally {
            is.close()
        }

        World world = new World()
        world.name = yaml.get(KEY_NAME) as String
        world.author = yaml.get('author') as String
        world.description = yaml.get(KEY_DESCRIPTION) as String

        readRooms(yaml, world)
        readPersonas(yaml, world)
        readPlayers(yaml, world)
        readExtras(yaml, world)
        readGoals(yaml, world)

        return world
    }

    private void readPersonas(Map<String, Object> yaml, World world) {
        Map<String, Object> personas = (Map<String, Object>) yaml.get('personas')
        if (!personas) {
            throw new IllegalArgumentException('At least one persona is required')
        }
        for (Map.Entry<String, Object> personaMap : personas.entrySet()) {
            Map<String, Object> data = (Map<String, Object>) personaMap.value
            Persona p = new Persona(name: personaMap.key)
            p.health = data.get('health') as Integer
            p.wealth = new BigDecimal(data.get('wealth').toString())
            world.personas.add(p)
        }
    }

    private void readPlayers(Map<String, Object> yaml, World world) {
        List<PlayerTemplate> players = parsePlayers(
                PlayerTemplate,
                world,
                (List<Map<String, Object>>) yaml.get('players')) {
            PlayerTemplate player, Map<String, Object> data ->
            if (data.containsKey(KEY_QUANTITY)) {
                player.quantity = parseQuantity(data.get(KEY_QUANTITY) as String)
            }
        }
        world.players.addAll(players)
    }

    private void readExtras(Map<String, Object> yaml, World world) {
        List<ExtrasTemplate> extras = parsePlayers(
                ExtrasTemplate,
                world,
                (List<Map<String, Object>>) yaml.get('extras')) {
            ExtrasTemplate extra, Map<String, Object> data ->
            if (data.containsKey(KEY_QUANTITY)) {
                extra.quantity = data.get(KEY_QUANTITY) as int
            }
        }
        world.extras.addAll(extras)
    }

    private <T extends CharacterTrait> List<T> parsePlayers(
            Class<T> template,
            World world,
            List<Map<String, Object>> players,
            Closure customizer) {
        List<T> result = []
        if (!players) {
            return result
        }

        for (Map<String, Object> data : players) {
            String personaStr = data.get('persona') as String
            Optional<Persona> persona = world.personas.stream()
                    .filter { it.name.equalsIgnoreCase(personaStr) }
                    .findFirst()
            if (!persona.present) {
                throw new IllegalArgumentException("Persona ${personaStr} not found")
            }
            T player = template.newInstance(
                    persona: persona.get(),
                    nickName: data.get('nickname'),
                    fullName: data.get('fullname'))
            String roomName = data.get(KEY_ROOM) as String
            if (roomName) {
                player.room = world.findRoomById(roomName).orElseThrow {
                    new IllegalArgumentException("Cannot find room ${roomName} for player ${player.fullName}")
                }
            }
            if (customizer) {
                customizer.call(player, data)
            }
            result << player
        }

        result
    }

    private IntRange parseQuantity(String quantityStr) {
        if (!quantityStr) {
            return 1..1
        }
        Range<Integer> quantity
        String[] split = quantityStr.split(/-/)
        if (split.length == 0) {
            quantity = 1..1
        } else if (split.length == 1) {
            quantity = (split[0] as Integer)..(split[0] as Integer)
        } else if (split.length == 2) {
            quantity = (split[0] as Integer)..(split[1] as Integer)
        } else {
            throw new IllegalArgumentException(
                    "Quantity expected to be a single value or range: 2, 1-10, got '${quantityStr}'")
        }
        quantity
    }

    private void readRooms(Map<String, Object> yaml, World world) {
        Map<String, Object> rooms = (Map<String, Object>) yaml.get('rooms')
        if (!rooms) {
            return
        }

        // first pass, create the rooms
        for (Map.Entry<String, Object> roomMap : rooms.entrySet()) {
            String roomId = roomMap.key
            Map<String, Object> data = (Map<String, Object>) roomMap.value
            Room room = new Room()
            room.id = roomId
            room.name = data.get(KEY_NAME) as String
            room.description = data.get(KEY_DESCRIPTION) as String
            if (room.name == null) {
                room.name = room.id.replaceAll(/[_-]+/, ' ').replaceAll(/\b([a-z])/) { it[1].toUpperCase() }
            }
            world.rooms.add(room)
        }

        // second pass, create neighbors
        for (Map.Entry<String, Object> roomMap : rooms.entrySet()) {
            String name = roomMap.key
            Map<String, Object> data = (Map<String, Object>) roomMap.value
            Room room = world.findRoomById(name)
                    .orElseThrow { new IllegalArgumentException("Cannot find room ${name}") }
            List<Map<String, Object>> neighbors = (List<Map<String, Object>>) data.getOrDefault(
                    'neighbors', Collections.emptyList())
            for (Map<String, Object> neighbor : neighbors) {
                String direction = neighbor.get('direction') as String
                if (!direction) {
                    throw new IllegalArgumentException("Direction is required for neighbor to room ${room.name}")
                }
                String roomName = neighbor.get(KEY_ROOM) as String
                Room neighborRoom = world.findRoomById(roomName)
                        .orElseThrow { new IllegalArgumentException("Cannot find room ${roomName}") }
                room.addNeighbor(direction, neighborRoom)

                String back = (neighbor.get('back') as String)?.toLowerCase()
                if (back) {
                    neighborRoom.addNeighbor(back, room)
                } else {
                    Optional<Direction> opposite = Direction.opposite(direction)
                    opposite.ifPresent { neighborRoom.addNeighbor(it.name().toLowerCase(), room) }
                }
            }
        }
    }

    private void readGoals(Map<String, Object> yaml, World world) {
        Map<String, Object> goals = (Map<String, Object>) yaml.get('goals') ?: [:]
        for (Map.Entry<String, Object> goalMap : goals.entrySet()) {
            Map<String, Object> data = (Map<String, Object>) goalMap.value
            Goal g = new Goal(name: goalMap.key)
            g.required = data.getOrDefault('required', false) as Boolean
            g.theEnd = data.getOrDefault('the-end', false) as Boolean
            Collection rules = data.get('rules', []) as Collection
            g.rules.addAll(rules*.toString())
            world.goals << g
        }
    }
}
