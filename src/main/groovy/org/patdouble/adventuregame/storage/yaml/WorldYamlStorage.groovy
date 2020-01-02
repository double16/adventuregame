package org.patdouble.adventuregame.storage.yaml

import org.patdouble.adventuregame.model.CharacterTrait
import org.patdouble.adventuregame.model.Direction
import org.patdouble.adventuregame.model.ExtrasTemplate
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World
import org.yaml.snakeyaml.Yaml

class WorldYamlStorage {
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
        world.setName(yaml.get('name') as String)
        world.setAuthor(yaml.get('author') as String)
        world.setDescription(yaml.get('description') as String)

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
        for(Map.Entry<String, Object> personaMap : personas.entrySet()) {
            Map<String, Object> data = (Map<String, Object>) personaMap.getValue()
            Persona p = new Persona(personaMap.getKey())
            p.setHealth(data.get('health') as Integer)
            p.setWealth(new BigDecimal(data.get('wealth').toString()))
            world.getPersonas().add(p)
        }
    }

    private void readPlayers(Map<String, Object> yaml, World world) {
        List<PlayerTemplate> players = parsePlayers(PlayerTemplate, world, (List<Map<String, Object>>) yaml.get('players')) {
            PlayerTemplate player, Map<String, Object> data ->
                if (data.containsKey('quantity')) {
                    player.quantity = parseQuantity(data.get('quantity') as String)
                }
        }
        world.getPlayers().addAll(players)
    }

    private void readExtras(Map<String, Object> yaml, World world) {
        List<ExtrasTemplate> extras = parsePlayers(ExtrasTemplate, world, (List<Map<String, Object>>) yaml.get('extras')) {
            ExtrasTemplate extra, Map<String, Object> data ->
                if (data.containsKey('quantity')) {
                    extra.quantity = data.get('quantity') as int
                }
        }
        world.getExtras().addAll(extras)
    }

    private <T extends CharacterTrait> List<T> parsePlayers(Class<T> template, World world, List<Map<String, Object>> players, Closure customizer) {
        List<T> result = []
        if (!players) {
            return result
        }

        for(Map<String, Object> data : players) {
            String personaStr = data.get('persona') as String
            Optional<Persona> persona = world.getPersonas().stream()
                    .filter{it.name.equalsIgnoreCase(personaStr)}
                    .findFirst()
            if (!persona.isPresent()) {
                throw new IllegalArgumentException("Persona ${personaStr} not found")
            }
            T player = template.newInstance(persona: persona.get(), nickName: data.get("nickname"), fullName: data.get("fullname"))
            String roomName = data.get("room") as String
            if (roomName) {
                player.setRoom(world.findRoomById(roomName).orElseThrow{new IllegalArgumentException("Cannot find room $roomName for player ${player.fullName}")})
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
            throw new IllegalArgumentException("Quantity expected to be a single value or range: 2, 1-10, got '${quantityStr}'")
        }
        quantity
    }

    private void readRooms(Map<String, Object> yaml, World world) {
        Map<String, Object> rooms = (Map<String, Object>) yaml.get("rooms")
        if (!rooms) {
            return
        }

        // first pass, create the rooms
        for(Map.Entry<String, Object> roomMap : rooms.entrySet()) {
            String roomId = roomMap.getKey()
            Map<String, Object> data = (Map<String, Object>) roomMap.getValue()
            Room room = new Room()
            room.id = roomId
            room.name = data.get('name') as String
            room.description = data.get('description') as String
            if (room.name == null) {
                room.name = room.id.replaceAll(/[_-]+/, ' ').replaceAll(/\b([a-z])/, { it[1].toUpperCase() })
            }
            world.getRooms().add(room)
        }

        // second pass, create neighbors
        for(Map.Entry<String, Object> roomMap : rooms.entrySet()) {
            String name = roomMap.getKey()
            Map<String, Object> data = (Map<String, Object>) roomMap.getValue()
            Room room = world.findRoomById(name).orElseThrow{new IllegalArgumentException("Cannot find room ${name}")}
            List<Map<String, Object>> neighbors = (List<Map<String, Object>>) data.getOrDefault("neighbors", Collections.emptyList())
            for(Map<String, Object> neighbor : neighbors) {
                String direction = neighbor.get('direction') as String
                if (!direction) {
                    throw new IllegalArgumentException("Direction is required for neighbor to room ${room.name}")
                }
                Room neighborRoom = world.findRoomById(neighbor.get('room') as String)
                        .orElseThrow{new IllegalArgumentException("Cannot find room ${neighbor.get('room')}")}
                room.addNeighbor(direction, neighborRoom)

                if (neighbor.containsKey('back')) {
                    neighborRoom.addNeighbor((neighbor.get('back') as String).toLowerCase(), room)
                } else {
                    Optional<Direction> opposite = Direction.opposite(direction)
                    opposite.ifPresent{ neighborRoom.addNeighbor(it.name().toLowerCase(), room) }
                }
            }
        }
    }

    private void readGoals(Map<String, Object> yaml, World world) {
        Map<String, Object> goals = (Map<String, Object>) yaml.get('goals') ?: [:]
        for(Map.Entry<String, Object> goalMap : goals.entrySet()) {
            Map<String, Object> data = (Map<String, Object>) goalMap.getValue()
            Goal g = new Goal(goalMap.getKey())
            g.required = data.getOrDefault('required', false) as Boolean
            g.theEnd = data.getOrDefault('the-end', false) as Boolean
            world.goals << g
        }
    }
}
