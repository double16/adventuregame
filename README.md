# adventuregame

(Not to be confused with Colossal Cave Adventure, Microsoft Adventure, Atari Adventure, et al.; the name probably needs to be changed).

Interactive Fiction Game for fun and learning.

## Worlds

Worlds are defined using a language based on Lua 5.1. For help with Lua see:
- https://www.lua.org/manual/5.1/manual.html
- https://www.tutorialspoint.com/lua/index.htm

TODO ...

### Validation

A world can be validated using the `validate-world` command. Supply the names of worlds to validate after the command.

Validation provides:
* Summary of the size of the world
* Map that can be visualized using the [GraphViz](https://www.graphviz.org) tool.
* Full AI runs with goal probabilities.
* Island detection. More than 1 island indicates there are places on the map that can't be reached.
* A ZIP file containing all of this information in JSON including full history of each run.
 
```shell
$ java -jar adventuregame.jar validate-world "Trailer Park"
Validating Trailer Park
Compiling... OK
0 regions, 7 rooms, 3 personas, 3 players
Island count: 2  WARN
Generating map ... OK
Determining goal probability ...
Progress:   100/100
Elapsed:    1:43
Goal one:   100/100
Goal two:   100/100
Goal three: 0/100
Goal four:  0/100
Memory:     464M/512M
OK
Trailer_Park_b30b43b3fcd36785b28e5c69804a9880ac03c0d617f3a6490b198ee67d4a3334.zip 
```

The number of runs can be specified using the `--runs` parameter.

```shell
$ java -jar adventuregame.jar validate-world --runs 1000 "Trailer Park"
Validating Trailer Park
Compiling... OK
0 regions, 7 rooms, 3 personas, 3 players
Island count: 2  WARN
Generating map ... OK
Determining goal probability ...
Progress:   100/1000
Elapsed:    1:43
Goal one:   100/1000
Goal two:   100/1000
Goal three: 0/1000
Goal four:  0/1000
Memory:     464M/512M
OK
Trailer_Park_b30b43b3fcd36785b28e5c69804a9880ac03c0d617f3a6490b198ee67d4a3334.zip 
```

## Play

Visiting the home page will present a list of available worlds. Select one to create a story. Once the story is created a URL will be displayed that can be sent to people to invite to the story.

Click the link, a list of players will be presented. Choose a player, a link will be displayed that enters the story as that player. **Bookmark** this link to continue the story later.

To begin the story, someone must click *start*. Any players not picked will become AI players.

At the end of the story a link is provided to view the story manuscript. This link can be shared and re-visited.

## Contributing

Helpful docs:

- [Drools Rule Engine](https://docs.jboss.org/drools/release/7.39.0.Final/drools-docs/html_single/index.html)
- [Lua](https://www.lua.org/manual/5.1/manual.html)
- [Vue.js](https://vuejs.org/v2/api)
- [Bootstrap](https://getbootstrap.com/docs/4.1/)
- [Font Awesome](https://fontawesome.com/icons?d=gallery&m=free)
- [Dagre D3](https://github.com/dagrejs/dagre-d3/wiki) (JavaScript graphing)
