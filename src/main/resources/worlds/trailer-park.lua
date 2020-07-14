world {
    name = 'Trailer Park';
    description = 'A testing environment';
    author = 'double16';
}

persona 'warrior' {
    health = 900;
    wealth = 50;
}

persona 'thief' {
    health = 600;
    wealth = 100;
}

persona 'thug' {
    health = 400;
    wealth = 20;
}

-- Names from https://www.fantasynamegenerators.com

player {
    persona = 'warrior';
    nickname = 'Shadowblow';
    fullname = 'Shadowblow the Hammer';
    room = 'entrance';
}

player {
    persona = 'thief';
    nickname = 'Victor';
    fullname = 'Victor the Spider';
    room = 'entrance';
    memory {
        rooms = { 'entrance', 'trailer_1', 'trailer_2', 'trailer_3', 'trailer_4', 'dump' }
    }
}

player {
    persona = 'thug';
    room = 'dump';
    quantity = '0-10';
}

extra {
    persona = 'thug';
    fullname = 'Thug';
    quantity = 5;
    room = 'dump';
    --goal {
    --
    --}
}

extra {
    persona = 'thug';
    fullname = 'Thug';
    quantity = 3;
    room = 'entrance';
    goal {
        description = 'Find the dump.';
        required = true;
        rule { 'player goes to room "dump"' }
    }
}

room 'entrance' {
    name = 'Entrance';
    description = 'The entrance to the trailer park has a broken gate and a cluster of mail boxes.';
    neighbor {
        direction = 'north';
        room = 'trailer_2';
    }
}

room 'trailer_1' {
    description = 'Trailer 1';
}

room 'trailer_2' {
    description = 'Trailer 2';
    neighbor {
        direction = 'west';
        room = 'trailer_1';
    };
    neighbor {
        direction = 'east';
        room = 'trailer_3';
    };
}

room 'trailer_3' {
    description = 'Trailer 3';
    neighbor {
        direction = 'east';
        room = 'trailer_4';
    }
}

room 'trailer_4' {
    description = 'Trailer 4';
    neighbor {
        direction = 'north';
        back = 'down';
        room = 'dump';
    }
}

room 'dump' {
    description = 'Trash Dump';
    neighbor {
        direction = 'dive';
        room = 'trailer_3';
    }
}

room 'nowhere' {
    description = 'This is a room with no paths leading to it.'
}

goal 'one' {
    description = 'Any player reaches trailer 2.';
    required = false;
    the_end = false;
    rule { 'player enters room "trailer_2"' }
}

goal 'two' {
    description = 'Any player reaches trailer 4.';
    required = false;
    the_end = false;
    rule { 'player enters room "trailer_4"' }
}

goal 'three' {
    description = 'Unspecified';
    required = true;
    the_end = false;
}

goal 'four' {
    description = 'Unspecified';
    required = true;
    the_end = true;
}
