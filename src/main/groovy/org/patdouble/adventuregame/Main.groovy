package org.patdouble.adventuregame

import org.patdouble.adventuregame.model.*
import org.patdouble.adventuregame.state.Motivator
import org.patdouble.adventuregame.state.Player
import org.patdouble.adventuregame.state.History
import org.patdouble.adventuregame.storage.yaml.WorldYamlStorage

class Main {

    static void main(String[] args) throws IOException {
        Console console = System.console()
        String playerName = System.getenv("USER")

        InputStream is = Main.class.getResourceAsStream("/worlds/middle-earth.yml")
        World world = new WorldYamlStorage().load(is)

        console.format("Welcome to %s!\n", world.getName())

        console.format("Who would you like to be, "+playerName+", "+ world.getPersonas()+" ? ")
        Persona persona = null
        while (persona == null) {
            console.flush()
            String line = console.readLine()
            for(Persona p :  world.getPersonas()) {
                if (p.getName().equalsIgnoreCase(line)) {
                    persona = p
                    break
                }
            }
            if (persona == null) {
                console.writer().println("You must choose from: "+ world.getPersonas())
            }
        }

        Player player = new Player(Motivator.HUMAN, persona, playerName)
        History story = new History(world, Collections.singletonList(player))

        for(Challenge challenge : world.getChallenges()) {
            Choice choice = null
            while (choice == null) {
                try {
                    console.format("\n%s\n%s\n", player.getStatus(), challenge.getDescription())
                    for(Choice ch : challenge.getChoices()) {
                        console.writer().print(ch.toString())
                        console.writer().print(", ")
                    }
                    console.writer().print(" ? ")

                    console.flush()
                    String action = console.readLine().toLowerCase()
                    for(Choice ch : challenge.getChoices()) {
                        if (ch.getAction().equalsIgnoreCase(action)) {
                            choice = ch
                        }
                    }
                    if (choice == null) {
                        console.format("You can't do that here!\n")
                    }
                } catch (IllegalArgumentException e) {
                    console.format("You can't do that here!\n")
                }
            }

            try {
                story.addStoryLine(choice.apply(player))
            } catch (IllegalStateException e) {
                // this means the adventure is over
                story.addStoryLine(e.getMessage())
            }
        }

        console.format("\nThe adventure is done. Here is your story.\n\n")

        for(String s : story.getEvents()) {
            console.writer().println(s)
        }
    }
}
