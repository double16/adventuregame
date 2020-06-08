regions = {};
rooms = {};
personas = {};
players = {};
extras = {};
goals = {};
world_table = {};

-- Returns a function that processes a domain params table. The array part of params are moved to a hash
-- by collecting into a key specified by 'target' in the table.
function domain_params(domain_name, domain)
    function add_to_target_list(x)
        local target = x.target
        assert(target ~= nil, domain_name..' array element in params expected to have "target" key, DSL implementation error')
        local target_list = domain[target]
        assert(target_list ~= nil, domain_name..' { '..target..' { ... } } is not valid')
        table.insert(target_list, x)
    end

    return function(params)
        for k,v in pairs(params) do
            if type(k) ~= "number" then
                domain[k] = v
            elseif type(v) == "table" then
                if v.target then
                    add_to_target_list(v)
                end
                for i,x in ipairs(v) do
                    add_to_target_list(x)
                end
            end
        end
    end
end

function room(id)
    local domain = {
        id = id;
        -- used when embedded in another domain
        target = 'rooms';
        neighbors = {};
    }
    table.insert(rooms, domain)

    return domain_params('room', domain)
end

function region(id)
    local domain = {
        id = id;
    }
    table.insert(regions, domain)

    return domain_params('region', domain)
end

function neighbor(params)
    params.target = 'neighbors'
    return params
end

function goal(arg)
    local domain = {
        -- used when embedded in another domain
        target = 'goals';
        rules = {};
    }
    if type(arg) == 'table' then
        domain_params('goal', domain)(arg)
        return domain
    else
        domain['name'] = arg
        table.insert(goals, domain)
        return domain_params('goal', domain)
    end
end

function rule(params)
    local result = { }
    for i,x in ipairs(params) do
        table.insert(result, { target = 'rules', definition = x })
    end
    return result
end

function persona(name)
    local domain = {
        name = name;
        -- used when embedded in another domain
        target = 'personas';
    }
    table.insert(personas, domain)

    return domain_params('persona', domain)
end

function player(params)
    local domain = {
        goals = { };
        memories = { };
    }
    table.insert(players, domain)
    domain_params('player', domain)(params)
end

function memory(params)
    params.target = 'memories'
    return params
end

function extra(params)
    local domain = {
        goals = { };
        memories = { };
    }
    table.insert(extras, domain)
    domain_params('extra', domain)(params)
end

function world(params)
    domain_params('world', world_table)(params)
end

function apply(definition)
    return definition()
end

domain = {
    world = world_table;
    regions = regions;
    rooms = rooms;
    personas = personas;
    players = players;
    extras = extras;
    goals = goals;
    apply = apply;
}

return domain
