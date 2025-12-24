import core.AutograderBuddy;
import core.World;
import edu.princeton.cs.algs4.StdDraw;
import org.junit.jupiter.api.Test;
import tileengine.TERenderer;
import tileengine.TETile;

import java.awt.*;
import java.util.Random;

public class WorldGenTests {
    @Test
    public void basicTest() {
        // put different seeds here to test different worlds
        TETile[][] tiles = AutograderBuddy.getWorldFromInput("n1234567890123456789s");

        TERenderer ter = new TERenderer();
        ter.initialize(tiles.length, tiles[0].length);
        ter.renderFrame(tiles);
        StdDraw.pause(5000); // pause for 5 seconds so you can see the output
    }

    @Test
    public void basicInteractivityTest() {
        TETile[][] tiles = AutograderBuddy.getWorldFromInput("n1231111111111111111111111111546swasdwasddddeeewdww:q");

        TERenderer ter = new TERenderer();
        ter.initialize(tiles.length, tiles[0].length);
        ter.renderFrame(tiles);
        StdDraw.pause(2000);
    }

    @Test
    public void basicSaveTest() {
        TETile[][] tiles = AutograderBuddy.getWorldFromInput("n123swasdwasddddeeewdww:q");

        TERenderer ter = new TERenderer();
        ter.initialize(tiles.length, tiles[0].length);
        ter.renderFrame(tiles);
        StdDraw.pause(2000);
        StdDraw.clear(Color.cyan);
        StdDraw.show();
        StdDraw.pause(100);
        tiles = AutograderBuddy.getWorldFromInput("n123swasdwasd:q");
        tiles = AutograderBuddy.getWorldFromInput("ldddeeewdww");
        ter.renderFrame(tiles);
        StdDraw.pause(2000);
    }

    @Test
    public void idk() {
        AutograderBuddy.getWorldFromInput("n8004217737854698935s");
        AutograderBuddy.getWorldFromInput("N4856161560646046006S");
    }

    @Test
    public void multipleGeneration() {
        Random rdm = new Random();
//        TERenderer ter = new TERenderer();
        for (int i = 0, j = 0; i < 1500000; i++) {
            long time = System.currentTimeMillis();
            World world = new World(rdm.nextLong());
            long duration = System.currentTimeMillis() - time;
//            System.out.println(duration);
//            if (duration > 10) {
//                System.out.println(i + "\t" + ++j + "\t" + duration + "\n");
//            }
//            ter.initialize(world.getWidth(), world.getHeight());
//            ter.renderFrame(world.getBoard());
//            System.out.println(world.getSeed());
//            StdDraw.pause(2000);
        }
    }

    String[] trickySeeds = new String[]{"-2131569457823262528", "2027739922450493747", "6761467943615784159",
        "-7595179269152698292", "-4380091093697992216", "4127133868420372896", "8934313649586369459",
        "5561269833210924221", "-2384379811550839861"},
        notThatTrickyButStillWierdSeeds = new String[]{"1838870606100659888", "6764725693684033236"};
    @Test
    public void testseed() {
        long duration = System.currentTimeMillis();
        World world = new World(Long.parseLong(trickySeeds[trickySeeds.length - 1]));
        System.out.println(System.currentTimeMillis() - duration);
        TERenderer ter = new TERenderer();
        ter.initialize(world.getWidth(), world.getHeight());
        ter.renderFrame(world.getBoard());
        StdDraw.pause(10000);
    }

    @Test
    public void shit() {
        World world = new World(Long.parseLong("-1668523966227595867"));
        TERenderer ter = new TERenderer();
        ter.initialize(world.getWidth(), world.getHeight());
        ter.renderFrame(world.getBoard());
        StdDraw.pause(1500);
    }
    @Test
    public void shit2() {
        World world = new World(Long.parseLong("-8181359712598829250"));
        TERenderer ter = new TERenderer();
        ter.initialize(world.getWidth(), world.getHeight());
        ter.renderFrame(world.getBoard());
        StdDraw.pause(1500);
    }
    @Test
    public void shit3() {
        World world = new World(Long.parseLong("-5725477770560527801"));
        world.runGame();
        TERenderer ter = new TERenderer();
        ter.initialize(world.getWidth(), world.getHeight());
        ter.renderFrame(world.getBoard());
        StdDraw.pause(1500);
    }

}
