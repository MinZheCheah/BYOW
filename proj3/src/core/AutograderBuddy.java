package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.math.BigInteger;

public class AutograderBuddy {

    /**
     * Simulates a game, but doesn't render anything or call any StdDraw
     * methods. Instead, returns the world that would result if the input string
     * had been typed on the keyboard.
     *
     * Recall that strings ending in ":q" should cause the game to quit and
     * save. To "quit" in this method, save the game to a file, then just return
     * the TETile[][]. Do not call System.exit(0) in this method.
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */
    public static TETile[][] getWorldFromInput(String input) {
        input = input.toUpperCase();
        int i = 0,
            inputLength = input.length();
        World worldFromInput = null;
        if (input.charAt(i) == 'L') {
            worldFromInput = new World(World.SAVE_FILE);
        } else if (input.charAt(i) == 'N') {
            StringBuilder seedBuilder = new StringBuilder();
            for (char charAtI = input.charAt(++i); '0' <= charAtI && charAtI <= '9'; charAtI = input.charAt(++i)) {
                seedBuilder.append(charAtI);
            }
            if (input.charAt(i) != 'S') {
                throw new IllegalArgumentException("Seed should be followed by an S");
            }
            long seed = new BigInteger(seedBuilder.toString()).longValue();
            worldFromInput = new World(seed);
        } else if (input.charAt(i) == 'Q') {
            System.exit(0);
        } else {
            throw new IllegalArgumentException("Input should start with N, L or Q (uppercase or lowercase).");
        }
        for (i++; i < inputLength; i++) {
            if (input.charAt(i) == ':') {
                if (String.valueOf(input.charAt(i + 1)).equalsIgnoreCase("Q")) {
                    worldFromInput.saveGame();
                    return worldFromInput.getBoard();
                } else {
                    throw new IllegalArgumentException("Any colon in the input must be followed by a 'Q' or 'q'.");
                }
            }
            worldFromInput.handleKeyInput(String.valueOf(input.charAt(i)));
        }
        return worldFromInput.getBoard();
    }




    /**
     * Used to tell the autograder which tiles are the floor/ground (including
     * any lights/items resting on the ground). Change this
     * method if you add additional tiles.
     */
    public static boolean isGroundTile(TETile t) {
        return t.character() == Tileset.FLOOR.character()
                || t.character() == Tileset.AVATAR.character()
                || t.character() == Tileset.FLOWER.character();
    }

    /**
     * Used to tell the autograder while tiles are the walls/boundaries. Change
     * this method if you add additional tiles.
     */
    public static boolean isBoundaryTile(TETile t) {
        return t.character() == Tileset.WALL.character()
                || t.character() == Tileset.LOCKED_DOOR.character()
                || t.character() == Tileset.UNLOCKED_DOOR.character();
    }
}
