world {
    name = 'The Hobbit';
    description = 'The Hobbit by J.R.R. Tolkien';
    author = 'double16';
}

persona 'warrior' {
    health = 100;
    wealth = 50;
}

persona 'thief' {
    health = 60;
    wealth = 100;
}

persona 'elf' {
    health = 100;
    wealth = 100;
}

persona 'hobbit' {
    health = 80;
    wealth = 30;
}

persona 'wizard' {
    health = 100;
    wealth = 30;
}

persona 'orc' {
    health = 50;
    wealth = 0;
}

player {
    persona = 'hobbit';
    nickname = 'Bilbo';
    fullname = 'Bilbo Baggins';
    room = 'bag_end_foyer';

    memory {
        regions = { 'shire' };
    }
}

player {
    persona = 'wizard';
    nickname = 'Gandalf';
    fullname = 'Gandalf the Grey';
    room = 'bag_end_foyer';

    memory {
        regions = { 'shire' };
        rooms = 'blackgate';
    }
}

extra {
    persona = 'orc';
    fullname = 'Orc';
    room = 'blackgate';
    quantity = 50;
    memory {
        regions = 'mordor';
    }
}

region 'shire' {
    name = 'The Shire';
}

region 'bag_end' {
    name = 'Bag End';
    inside = 'shire';
    description = "Baggins' Hobbit Hole";
}

region 'mordor' {
    name = 'Mordor';
}

room 'bag_end_foyer' {
    description = "Entrance to Bag End";
    region = 'bag_end';
}

room 'bag_end_kitchen' {
    description = "Bag End Kitchen";
    region = 'bag_end';
}

room 'blackgate' {
    description = "Entrance to Mordor";
    region = 'mordor';
}
