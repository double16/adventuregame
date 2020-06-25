world {
    name = 'The Hobbit';
    description = 'The Hobbit by J.R.R. Tolkien';
    author = 'double16';
}

require 'worlds.middle-earth'

player {
    persona = 'hobbit';
    nickname = 'Bilbo';
    fullname = 'Bilbo Baggins';
    room = 'bag_end_garden';

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

room 'blackgate' {
    description = "Entrance to Mordor";
    region = 'mordor';
}
