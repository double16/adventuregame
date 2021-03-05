--
-- Middle Earth base file
--

persona 'elf' {
    health = 900;
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

persona 'ranger' {
    health = 750;
    wealth = 50;
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

room 'hobbiton_east' {
    region = 'shire';
}

room 'east_road_1' {
    region = 'east';
    neighbor {
        direction = 'west';
        room = 'hobbiton_east';
    };
    neighbor {
        direction = 'east';
        room = 'bree_main_road';
    };
}

region 'old_forest' {
    name = 'Old Forest';
    inside = 'west';
    description = 'Remnants of the old forest east of the Shire.';
}

region 'bree' {
    name = 'Bree';
    inside = 'west';
    description = 'Town of Bree';
}

room 'bree_main_road' {
    name = 'Main Road of Bree';
    inside = 'bree';
    neighbor {
        direction = 'north';
        room = 'prancing_pony';
    }
}

room 'prancing_pony' {
    name = 'Prancing Pony';
    inside = 'bree';
}

--
-- The East
--

region 'east' {
    name = 'The East';
}

region 'moria' {
    name = 'Moria';
    inside = 'east';
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
