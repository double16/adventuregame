--
-- Middle Earth base file
--

persona 'elf' {
    health = 1000;
    wealth = 500;
}

persona 'hobbit' {
    health = 800;
    wealth = 300;
}

persona 'wizard' {
    health = 1000;
    wealth = 300;
}

persona 'dwarf' {
    health = 700;
    wealth = 900;
}

persona 'orc' {
    health = 300;
    wealth = 20;
}

--
-- The West
--

region 'west' {
    name = 'The West';
}

region 'shire' {
    name = 'The Shire';
    inside = 'west';
}

region 'hobbiton' {
    name = 'Hobbiton';
    inside = 'shire';
    description = 'Town of Hobbiton';
}

region 'bag_end' {
    name = 'Bag End';
    inside = 'hobbiton';
    description = "Baggins' Hobbit Hole";
}

room 'bag_end_foyer' {
    name = 'Bag End Foyer';
    description = "Entrance to Bag End";
    region = 'bag_end';
    neighbor {
        direction = 'west';
        room = 'bag_end_kitchen';
    };
    neighbor {
        direction = 'east';
        room = 'bag_end_parlor';
    };
}

room 'bag_end_kitchen' {
    name = 'Bag End Kitchen';
    description = "Bag End Kitchen";
    region = 'bag_end';
}

room 'bag_end_parlor' {
    name = 'Bag End Parlor';
    description = "Bag End Parlor";
    region = 'bag_end';
}

room 'bag_end_garden' {
    name = 'Bag End Garden';
    description = 'Bag End Garden';
    region = 'bag_end';
    neighbor {
        direction = 'north';
        room = 'bag_end_foyer';
    };
}

region 'bree' {
    name = 'Bree';
    inside = 'west';
    description = 'Town of Bree';
}

region 'old_forest' {
    name = 'Old Forest';
    inside = 'west';
    description = 'Remnants of the old forest north of the Shire.';
}

--
-- The East
--

region 'east' {
    name = 'The East';
}

--
-- The North
--

region 'north' {
    name = 'The North';
}

--
-- The South
--

region 'south' {
    name = 'The South';
}

region 'mordor' {
    name = 'Mordor';
    inside = 'south';
}

