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
    room = 'bag_end';
}

player {
    persona = 'wizard';
    nickname = 'Gandalf';
    fullname = 'Gandalf the Grey';
    room = 'bag_end';
}

extra {
    persona = 'orc';
    fullname = 'Orc';
    room = 'blackgate';
    quantity = 50;
}

room 'bag_end' {
    description = "Baggins' Hobbit Hole"
}

room 'blackgate' {
    description = "Entrance to Mordor"
}
