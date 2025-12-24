package core;

import edu.princeton.cs.algs4.StdDraw;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.math.BigInteger;
import java.util.Random;

public class Menu {
    public static final int DEFAULT_CANVAS_WIDTH = 1080;
    public static final int DEFAULT_CANVAS_HEIGHT = 675;

    private int canvasWidth;
    private int canvasHeight;

    private World world;

    private String[] args;
    boolean isFirstVist = true;
    String pngFile = null;
    private String userInputSeed = "";
    private boolean userPressedN = false;
    private long timeWhenPressedN;
    private boolean isFirstN = true;

    public Menu(int w, int h, String[] args) {
        canvasWidth = w;
        canvasHeight = h;
        world = null;
        this.args = args;
    }

    public Menu(String[] args) {
        this(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_HEIGHT, args);
    }

    public Menu() {
        this(new String[]{});
    }

    public void runMenu() {
        if (isFirstVist) {
            StdDraw.setCanvasSize(canvasWidth, canvasHeight);
            StdDraw.setXscale(0, 1);
            StdDraw.setYscale(0, 1);
            StdDraw.clear(new Color(0, 0, 0));
            StdDraw.enableDoubleBuffering();
            isFirstVist = false;
        }

        while (true) {
            displayMainMenu();
            if (StdDraw.hasNextKeyTyped()) {
                handleKeyInput(StdDraw.nextKeyTyped());
            }
            if (world != null) {
                world.runGame();
            }
        }
    }

    private void displayMainMenu() {
        StdDraw.clear(new Color(0, 0, 0));
        StdDraw.setFont(new Font("Monaco", Font.BOLD, Math.min(canvasWidth, canvasHeight) / 10));
        StdDraw.setPenColor(new Color(255, 255, 255));
        StdDraw.text(.5, .75, "World of Tiles!!!!!");
        StdDraw.setFont(new Font("Arial", Font.PLAIN, Math.min(canvasWidth, canvasHeight) / 20));
        StdDraw.text(.5, .6, "New Game (N)");
        StdDraw.text(.5, .52, "Random World (S)");
        StdDraw.text(.5, .44, "Load Game (L)");
        StdDraw.text(.5, .36, "Customize Avatar Image (A)");
        StdDraw.text(.5, .28, "Quit (Q)");
        if (userPressedN) {
            StdDraw.setFont(new Font("Arial", Font.PLAIN, Math.min(canvasWidth, canvasHeight) / 25));
            StdDraw.textRight(.35, .18, "Seed: ");
            if (((System.currentTimeMillis() - timeWhenPressedN) / 500) % 2 == 0) {
                StdDraw.text(.5, .11, "Press 'S' to start");
            }
            if (!userInputSeed.isEmpty()) {
                StdDraw.textLeft(.35, .18, userInputSeed);
            }
        }

        StdDraw.show();
    }

    private void displayAvatarMenu() {
        StdDraw.clear(new Color(0, 0, 0));
        StdDraw.setFont(new Font("Monaco", Font.BOLD, Math.min(canvasWidth, canvasHeight) / 10));
        StdDraw.text(0.5, 0.75, "Avatar Customization");
        StdDraw.setFont(new Font("Arial", Font.PLAIN, Math.min(canvasWidth, canvasHeight) / 20));
        StdDraw.text(0.5, 0.55, "Press 'C' to Choose Avatar");
        StdDraw.text(0.5, 0.4, "Press 'B' to Go Back");
        StdDraw.show();
    }

    public void handleKeyInput(char inputKey) {
        if (userPressedN) {
            handleSeedInput(inputKey);
        }
        switch (String.valueOf(inputKey).toUpperCase()) {
            case "Q": {
                System.exit(0);
                return;
            }
            case "L":
                world = new World(World.SAVE_FILE, pngFile);
                return;
            case "S":
                long seed = !userInputSeed.isEmpty() && !userInputSeed.equals("-")
                        ? new BigInteger(userInputSeed).longValue()
                        : (args.length > 0 ? Long.parseLong(args[0]) : (new Random()).nextLong());
                world = new World(seed, pngFile);
                return;
            case "N":
                if (isFirstN) {
                    userPressedN = true;
                    timeWhenPressedN = System.currentTimeMillis();
                }
                return;
            case "A":
                handleAvatarMenuInput();
                return;
            default:
                return;
        }
    }

    private void handleSeedInput(char inputKey) {
        if (inputKey == '-' && userInputSeed.isEmpty()) {
            userInputSeed = "-";
        }
        if ('0' <= inputKey && inputKey <= '9') {
            userInputSeed += inputKey;
            return;
        }
        if (inputKey == '\b') {
            userInputSeed = userInputSeed.substring(0, Math.max(0, userInputSeed.length() - 1));
        }
    }



    public void handleAvatarMenuInput() {
        StdDraw.clear(new Color(0, 0, 0));
        displayAvatarMenu();
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                String key = String.valueOf(StdDraw.nextKeyTyped()).toUpperCase();
                if (key.equals("C")) {
                    pngFile = selectAvatarImage();
                } else if (key.equals("B")) {
                    runMenu();
                }
            }
        }
    }

    public String selectAvatarImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Avatar Image");

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile.getAbsolutePath();
        }
        return null;
    }

}
