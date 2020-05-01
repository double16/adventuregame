package org.patdouble.adventuregame.storage.lua

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaClosure
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Prototype
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.jse.JsePlatform
import org.patdouble.adventuregame.model.CharacterTrait
import org.patdouble.adventuregame.model.Direction
import org.patdouble.adventuregame.model.ExtrasTemplate
import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World

/**
 * Storage of World using Lua.
 */
@CompileDynamic
class WorldLuaStorage {
    public static final String KEY_NAME = 'name'
    public static final String KEY_AUTHOR = 'author'
    public static final String KEY_DESCRIPTION = 'description'
    public static final String KEY_QUANTITY = 'quantity'
    public static final String KEY_ROOM = 'room'

    final Prototype dslPrototype

    WorldLuaStorage() {
        dslPrototype = JsePlatform.standardGlobals().loadPrototype(getClass().getResourceAsStream('world-dsl.lua'), '@world-dsl.lua', 't')
    }

    World load(InputStream is) throws IOException {
        Objects.requireNonNull(dslPrototype)
        Objects.requireNonNull(is)
        Globals globals = JsePlatform.standardGlobals()
        LuaClosure f = new LuaClosure(dslPrototype, globals)
        LuaTable domain = f.call() as LuaTable
        LuaFunction apply = domain.get('apply') as LuaFunction
        Objects.requireNonNull(apply)
        LuaValue worldChunk = globals.load(is, 'world', 't', globals)
        apply.call(worldChunk)

        World world = new World()
        LuaTable worldDomain = domain.get('world') as LuaTable
        world.name = nullSafeToString(worldDomain.get(KEY_NAME))
        world.author = nullSafeToString(worldDomain.get(KEY_AUTHOR))
        world.description = nullSafeToString(worldDomain.get(KEY_DESCRIPTION))

        readRooms(domain, world)
        readPersonas(domain, world)
        readPlayers(domain, world)
        readExtras(domain, world)
        readGoals(domain, world)

        return world
    }

    @CompileStatic
    private static class LuaArrayIterator<T extends LuaValue> implements Iterator<T> {
        private final LuaTable table
        private LuaValue k = LuaValue.NIL
        private Varargs n

        LuaArrayIterator(LuaTable table) {
            this.table = (LuaTable) (table ?: new LuaTable())
            n = table.next(k)
        }

        @Override
        boolean hasNext() {
            !n.arg1().isnil()
        }

        @Override
        T next() {
            T result = n.arg(2) as T
            k = n.arg1()
            n = table.next(k)
            result
        }
    }

    private static String nullSafeToString(LuaValue v) {
        if (v == null || v.isnil()) {
            return null
        }
        v.toString()
    }

    private void readPersonas(LuaTable domain, World world) {
        LuaTable personas = domain.get('personas') as LuaTable
        if (personas.isnil() || personas.length() == 0) {
            throw new IllegalArgumentException('At least one persona is required')
        }
        for (LuaTable data : new LuaArrayIterator<LuaTable>(personas)) {
            Persona p = new Persona(name: data.get(KEY_NAME))
            p.health = data.get('health').toint()
            p.wealth = new BigDecimal(data.get('wealth').tofloat())
            world.personas.add(p)
        }
    }

    private void readPlayers(LuaTable domain, World world) {
        List<PlayerTemplate> players = parsePlayers(
                PlayerTemplate,
                world,
                domain.get('players') as LuaTable) {
            PlayerTemplate player, LuaTable data ->
                LuaValue quantity = data.get(KEY_QUANTITY)
                if (!quantity.isnil()) {
                    if (quantity.isnumber()) {
                        player.quantity = quantity.toint()
                    } else {
                        player.quantity = parseQuantity(quantity as String)
                    }
                }
        }
        world.players.addAll(players)
    }

    private void readExtras(LuaTable domain, World world) {
        List<ExtrasTemplate> extras = parsePlayers(
                ExtrasTemplate,
                world,
                domain.get('extras') as LuaTable) {
            ExtrasTemplate extra, LuaTable data ->
                LuaValue quantity = data.get(KEY_QUANTITY)
                if (!quantity.isnil()) {
                    extra.quantity = quantity.toint()
                }
        }
        world.extras.addAll(extras)
    }

    private <T extends CharacterTrait> List<T> parsePlayers(
            Class<T> template,
            World world,
            LuaTable players,
            Closure customizer) {
        List<T> result = []
        if (!players) {
            return result
        }

        for (LuaTable data : new LuaArrayIterator<LuaTable>(players)) {
            String personaStr = nullSafeToString(data.get('persona'))
            Optional<Persona> persona = world.personas.stream()
                    .filter { it.name.equalsIgnoreCase(personaStr) }
                    .findFirst()
            if (!persona.present) {
                throw new IllegalArgumentException("Persona ${personaStr} not found")
            }
            T player = template.newInstance(
                    persona: persona.get(),
                    nickName: nullSafeToString(data.get('nickname')),
                    fullName: nullSafeToString(data.get('fullname')))
            LuaValue roomNameValue = data.get(KEY_ROOM)
            if (!roomNameValue.isnil()) {
                String roomName = roomNameValue as String
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

    private void readRooms(LuaTable domain, World world) {
        LuaTable rooms = domain.get('rooms') as LuaTable
        if (rooms.isnil() || rooms.length() == 0) {
            return
        }

        // first pass, create the rooms
        for (LuaTable data : new LuaArrayIterator<LuaTable>(rooms)) {
            String roomId = nullSafeToString(data.get('id'))
            Room room = new Room()
            room.modelId = roomId
            room.name = nullSafeToString(data.get(KEY_NAME))
            room.description = nullSafeToString(data.get(KEY_DESCRIPTION))
            if (room.name == null) {
                room.name = room.modelId.replaceAll(/[_-]+/, ' ').replaceAll(/\b([a-z])/) { it[1].toUpperCase() }
            }
            world.rooms.add(room)
        }

        // second pass, create neighbors
        for (LuaTable data : new LuaArrayIterator<LuaTable>(rooms)) {
            String roomId = nullSafeToString(data.get('id'))
            Room room = world.findRoomById(roomId)
                    .orElseThrow { new IllegalArgumentException("Cannot find room ${roomId}") }
            LuaTable neighbors = data.get('neighbors') as LuaTable
            if (!neighbors.isnil()) {
                for (LuaTable neighbor : new LuaArrayIterator<LuaTable>(neighbors)) {
                    String direction = nullSafeToString(neighbor.get('direction'))
                    if (!direction) {
                        throw new IllegalArgumentException("Direction is required for neighbor to room ${room.name}")
                    }
                    String roomName = nullSafeToString(neighbor.get(KEY_ROOM))
                    Room neighborRoom = world.findRoomById(roomName)
                            .orElseThrow { new IllegalArgumentException("Cannot find room ${roomName}") }
                    room.addNeighbor(direction, neighborRoom)

                    String back = nullSafeToString(neighbor.get('back'))?.toLowerCase()
                    if (back) {
                        neighborRoom.addNeighbor(back, room)
                    } else {
                        Optional<Direction> opposite = Direction.opposite(direction)
                        opposite.ifPresent { neighborRoom.addNeighbor(it.name().toLowerCase(), room) }
                    }
                }
            }
        }
    }

    private void readGoals(LuaTable domain, World world) {
        LuaTable goals = domain.get('goals') as LuaTable
        if (goals.isnil()) {
            return
        }
        for (LuaTable data : new LuaArrayIterator<LuaTable>(goals)) {
            Goal g = new Goal(name: nullSafeToString(data.get(KEY_NAME)))
            g.description = nullSafeToString(data.get('description'))
            g.required = Optional.ofNullable(data.get('required')).orElse(LuaValue.FALSE).toboolean()
            g.theEnd = Optional.ofNullable(data.get('the_end')).orElse(LuaValue.FALSE).toboolean()

            for(LuaTable rule : new LuaArrayIterator<LuaTable>(data.get('rules') as LuaTable)) {
                g.rules.add(nullSafeToString(rule.get('definition')))
            }
            world.goals << g
        }
    }

}
