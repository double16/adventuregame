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
import org.patdouble.adventuregame.model.Direction

import org.patdouble.adventuregame.model.Goal
import org.patdouble.adventuregame.model.Persona
import org.patdouble.adventuregame.model.PlayerTemplate
import org.patdouble.adventuregame.model.Region
import org.patdouble.adventuregame.model.Room
import org.patdouble.adventuregame.model.World

import java.math.RoundingMode

/**
 * Storage of World using Lua.
 */
@CompileDynamic
class WorldLuaStorage {
    static final String KEY_NAME = 'name'
    static final String KEY_AUTHOR = 'author'
    static final String KEY_DESCRIPTION = 'description'
    static final String KEY_QUANTITY = 'quantity'
    static final String KEY_ROOM = 'room'
    static final String KEY_ID = 'id'
    static final String KEY_REGIONS = 'regions'
    static final String KEY_ROOMS = 'rooms'

    final Prototype dslPrototype

    WorldLuaStorage() {
        dslPrototype = JsePlatform.standardGlobals().loadPrototype(
                getClass().getResourceAsStream('world-dsl.lua'),
                '@world-dsl.lua', 't')
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

        readRegions(domain, world)
        readRooms(domain, world)
        readPersonas(domain, world)
        readPlayers(domain, world)
        readExtras(domain, world)
        readGoals(domain, world.goals)

        return world
    }

    @CompileStatic
    private static class LuaArrayIterator<T extends LuaValue> implements Iterator<T> {
        private LuaTable table
        private LuaValue k = LuaValue.NIL
        private Varargs n

        LuaArrayIterator(LuaValue value) {
            if (value instanceof LuaTable) {
                init(value as LuaTable)
            } else if (!value || value.isnil()) {
                init(new LuaTable())
            } else {
                LuaTable t = new LuaTable()
                t.set(0, value)
                init(t)
            }
        }

        LuaArrayIterator(LuaTable table) {
            init(table)
        }

        private void init(LuaTable table) {
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

    private static String humanReadableModelId(String modelId) {
        modelId.replaceAll(/[_-]+/, ' ')
                .replaceAll(/\b([a-z])/) { it[1].toUpperCase() }
    }

    private void readPersonas(LuaTable domain, World world) {
        LuaTable personas = domain.get('personas') as LuaTable
        if (personas.isnil() || personas.length() == 0) {
            throw new IllegalArgumentException('At least one persona is required')
        }
        for (LuaTable data : new LuaArrayIterator<LuaTable>(personas)) {
            Persona p = new Persona(name: data.get(KEY_NAME))
            p.health = data.get('health').toint()
            p.wealth = new BigDecimal(data.get('wealth').toString()).setScale(2, RoundingMode.DOWN)
            world.personas.add(p)
        }
    }

    private void readPlayers(LuaTable domain, World world) {
        List<PlayerTemplate> players = parsePlayers(
                world,
                domain.get('players') as LuaTable)
        world.players.addAll(players)
    }

    private void readExtras(LuaTable domain, World world) {
        List<PlayerTemplate> extras = parsePlayers(
                world,
                domain.get('extras') as LuaTable)
        world.extras.addAll(extras)
    }

    private List<PlayerTemplate> parsePlayers(
            World world,
            LuaTable players) {
        List<PlayerTemplate> result = []
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
            PlayerTemplate player = new PlayerTemplate(
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

            readGoals(data, player.goals)
            readKnownRooms(world, data, player.knownRooms)

            LuaValue quantity = data.get(KEY_QUANTITY)
            if (!quantity.isnil()) {
                if (quantity.isnumber()) {
                    player.quantity = new IntRange(quantity.toint(), quantity.toint())
                } else {
                    player.quantity = parseQuantity(quantity as String)
                }
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

    private void readRegions(LuaTable domain, World world) {
        LuaTable regions = domain.get(KEY_REGIONS) as LuaTable
        if (regions.isnil() || regions.length() == 0) {
            return
        }
        
        // first pass, create the regions
        for (LuaTable data : new LuaArrayIterator<LuaTable>(regions)) {
            String regionId = nullSafeToString(data.get(KEY_ID))
            Region region = new Region()
            region.modelId = regionId
            region.name = nullSafeToString(data.get(KEY_NAME))
            region.description = nullSafeToString(data.get(KEY_DESCRIPTION))
            if (region.name == null) {
                region.name = humanReadableModelId(region.modelId)
            }
            world.regions.add(region)
        }

        // second pass, associate parent regions
        for (LuaTable data : new LuaArrayIterator<LuaTable>(regions)) {
            String regionId = nullSafeToString(data.get(KEY_ID))
            Region region = world.findRegionById(regionId)
                    .orElseThrow { new IllegalArgumentException("Cannot find region ${regionId}") }
            String parentId = nullSafeToString(data.get('inside'))
            if (parentId) {
                if (parentId == regionId) {
                    new IllegalArgumentException("Region ${regionId} cannot be inside itself")
                }
                Region parentRegion = world.findRegionById(parentId)
                        .orElseThrow {
                            new IllegalArgumentException("Cannot find region ${parentId} for ${regionId}.inside") }
                // check for cycles
                Region cycleCheck = parentRegion
                while (cycleCheck) {
                    if (cycleCheck.parent?.modelId == parentRegion.modelId) {
                        new IllegalArgumentException(
                                "Region ${regionId} can not be inside region ${parentId}, causes a loop")
                    }
                    cycleCheck = cycleCheck.parent
                }
                region.parent = parentRegion
            }
        }
    }

    private void readRooms(LuaTable domain, World world) {
        LuaTable rooms = domain.get(KEY_ROOMS) as LuaTable
        if (rooms.isnil() || rooms.length() == 0) {
            return
        }

        // first pass, create the rooms
        for (LuaTable data : new LuaArrayIterator<LuaTable>(rooms)) {
            String roomId = nullSafeToString(data.get(KEY_ID))
            Room room = new Room()
            room.modelId = roomId
            room.name = nullSafeToString(data.get(KEY_NAME))
            room.description = nullSafeToString(data.get(KEY_DESCRIPTION))
            if (room.name == null) {
                room.name = humanReadableModelId(room.modelId)
            }
            String regionId = nullSafeToString(data.get('region'))
            if (regionId) {
                Region region = world.findRegionById(regionId)
                        .orElseThrow { new IllegalArgumentException("Cannot find region ${regionId}") }
                room.region = region
            }
            world.rooms.add(room)
        }

        // second pass, create neighbors
        for (LuaTable data : new LuaArrayIterator<LuaTable>(rooms)) {
            String roomId = nullSafeToString(data.get(KEY_ID))
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

    private Goal readGoal(LuaTable data) {
        Goal g = new Goal(name: nullSafeToString(data.get(KEY_NAME)))
        g.description = nullSafeToString(data.get(KEY_DESCRIPTION))
        g.required = Optional.ofNullable(data.get('required')).orElse(LuaValue.FALSE).toboolean()
        g.theEnd = Optional.ofNullable(data.get('the_end')).orElse(LuaValue.FALSE).toboolean()

        for(LuaTable rule : new LuaArrayIterator<LuaTable>(data.get('rules') as LuaTable)) {
            g.rules.add(nullSafeToString(rule.get('definition')))
        }
        g
    }

    private void readGoals(LuaTable domain, Collection<Goal> list) {
        LuaTable goals = domain.get('goals') as LuaTable
        if (goals.isnil()) {
            return
        }
        for (LuaTable data : new LuaArrayIterator<LuaTable>(goals)) {
            list << readGoal(data)
        }
    }

    private void readKnownRooms(World world, LuaTable domain, Collection<Room> list) {
        LuaValue memories = domain.get('memories')
        if (memories.isnil()) {
            return
        }

        for (LuaValue element : new LuaArrayIterator<LuaTable>(memories)) {
            for (LuaValue data : new LuaArrayIterator<LuaTable>(element.get(KEY_ROOMS))) {
                String modelId = data.toString()
                Room room = world.findRoomById(modelId).get()
                if (!room) {
                    throw new IllegalArgumentException("Cannot find room ${modelId} specified in memory")
                }
                list << room
            }

            for (LuaValue data : new LuaArrayIterator<LuaTable>(element.get(KEY_REGIONS))) {
                String modelId = data.toString()
                Region region = world.findRegionById(modelId).get()
                if (!region) {
                    throw new IllegalArgumentException("Cannot find region ${modelId} specified in memory")
                }
                list.addAll(world.findRoomsByRegion(region))
            }
        }
    }

}
