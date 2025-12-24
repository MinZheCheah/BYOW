package core;

import tileengine.TETile;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Avatar {
    private Point pos;
    private World world;
    private TETile[][] board;
    private final int fullHealth;
    private int health;
    private TETile tileRepresentation;
    private EntityType entityType;
    private BufferedImage avatarImage;
    private long prevLoseHealthTime = System.currentTimeMillis();
    private boolean isDead = false;
    protected enum EntityType {
        PLAYER, ENEMY, FOOD;
    }

    public Avatar(World world, int startX, int startY, int fullHealth, int currHealth, EntityType type, TETile tile) {
        this.world = world;
        this.pos = new Point(startX, startY);
        this.fullHealth = fullHealth;
        this.health = currHealth;
        this.entityType = type;
        this.tileRepresentation = tile;
    }

    public void setAvatarImage(BufferedImage image) {
        this.avatarImage = image;
    }

    public Avatar(World world, int startX, int startY, int fullHealth, EntityType type, TETile tile) {
        this(world, startX, startY, fullHealth, fullHealth, type, tile);
    }

    public Avatar(World world, int fullHealth, int currentHealth, EntityType type, TETile tile) {
        this.world = world;
        this.fullHealth = fullHealth;
        this.health = currentHealth;
        this.entityType = type;
        this.tileRepresentation = tile;

        World.RandomAssumedOnlyGeneratingIntJustForFileSaving random = world.getRandom();
        short[][] worldBoardStatus = world.getBoardStatus();
        int worldWidth = world.getWidth(),
            worldHeight = world.getHeight(),
            startX = random.nextInt(worldWidth),
            startY = random.nextInt(worldHeight);
        while (worldBoardStatus[startX][startY] != 2) {
            startX = random.nextInt(worldWidth);
            startY = random.nextInt(worldHeight);
        }
        this.pos = new Point(startX, startY);
        switch (entityType) {
            case PLAYER: {
                this.world.boardStatus[startX][startY] = 3;
                break;
            }
            case ENEMY: {
                this.world.boardStatus[startX][startY] = 4;
                break;
            }
            case FOOD: {
                this.world.boardStatus[startX][startY] = 5;
                break;
            }
            default: {
            }
        }

    }

    public Avatar(World world, int fullHealth, EntityType type, TETile tile) {
        this(world, fullHealth, fullHealth, type, tile);
    }

    public Point getPos() {
        return pos;
    }

    public static void drawOnBoard(Avatar avatar, TETile[][] tiles) {
        if (!avatar.isDead()) {
            tiles[avatar.pos.x][avatar.pos.y] = avatar.tileRepresentation;
        }
    }

    public static void drawOnBoard(Avatar[] avatars, TETile[][] tiles) {
        for (Avatar avatar : avatars) {
            if (!avatar.isDead()) {
                tiles[avatar.pos.x][avatar.pos.y] = avatar.tileRepresentation;
            }
            drawOnBoard(avatar, tiles);
        }
    }

    private void updateMovementAndDrawOnBoard(int dx, int dy) {
        if (isDead) {
            return;
        }
        world.board[pos.x][pos.y] = world.interior;
        world.boardStatus[pos.x][pos.y] = 2;
        pos.translate(dx, dy);
        world.board[pos.x][pos.y] = tileRepresentation;
        switch (entityType) {
            case PLAYER: {
                world.boardStatus[pos.x][pos.y] = 3;
                break;
            }
            case ENEMY: {
                world.boardStatus[pos.x][pos.y] = 4;
                break;
            }
            case FOOD: {
                world.boardStatus[pos.x][pos.y] = 5;
                break;
            }
            default: {
            }
        }
    }

    //Move entity based on input direction
    public boolean tryMove(char inputKeyDirection) {
        if (isDead) {
            return false;
        }
        int dx = 0,
            dy = 0;
        switch (String.valueOf(inputKeyDirection).toUpperCase()) {
            case "W":
                dy++;
                break;
            case "S":
                dy--;
                break;
            case "D":
                dx++;
                break;
            case "A":
                dx--;
                break;
            default: {
            }
        }
        boolean canMove = isValidMove(dx, dy);
        if (canMove) {
            updateMovementAndDrawOnBoard(dx, dy);
        }
        return canMove;
    }

    private boolean isValidMove(int dx, int dy) {
        return world.getBoardStatus()[this.pos.x + dx][this.pos.y + dy] > 1;
    }

    public static boolean collide(Avatar av1, Avatar av2) {
        if (av1.isDead || av2.isDead || !av1.pos.equals(av2.pos)) {
            return false;
        }
        if (av1.entityType.equals(EntityType.FOOD)) {
            loseHealthAndArrangeFuneralIfDead(av1);
            av2.tryIncreaseHealth();
            return true;
        }
        if (av2.entityType.equals(EntityType.FOOD)) {
            loseHealthAndArrangeFuneralIfDead(av2);
            av1.tryIncreaseHealth();
            return true;
        }
        if (!av1.entityType.equals(av2.entityType)) {
            loseHealthAndArrangeFuneralIfDead(av1);
            loseHealthAndArrangeFuneralIfDead(av2);
            return true;
        }
        return false;
    }

    private char intToMove(int n) {
        switch (Math.floorMod(n, 4)) {
            case 0 -> {
                return 'w';
            }
            case 1 -> {
                return 'a';
            }
            case 2 -> {
                return 's';
            }
            default -> {
                return 'd';
            }
        }
    }

    protected static void randomMove(Avatar[] avatars) {
        for (Avatar avatar : avatars) {
            if (avatar != null) {
                avatar.randomMove();
            }
        }
    }

    protected void randomMove() {
        if (!isDead) {
            World.RandomAssumedOnlyGeneratingIntJustForFileSaving random = world.getRandom();
            while (!tryMove(intToMove(random.nextInt()))) {
                continue;
            }
        }
    }

    public int getHealth() {
        return health;
    }

    public void tryIncreaseHealth() {
        health = Math.min(health + 1, fullHealth);
    }

    public static void loseHealthAndArrangeFuneralIfDead(Avatar av) {
        if (av.coldTime() > 667) {
            av.prevLoseHealthTime = System.currentTimeMillis();
            av.health--;
            if (av.health <= 0) {
                av.die();
            }
        }

    }

    public boolean isDead() {
        return isDead;
    }

    protected void die() {
        isDead = true;
    }

    private long coldTime() {
        return System.currentTimeMillis() - prevLoseHealthTime;
    }

    @Override
    public String toString() {
        return entityType + "\t" + pos.x + "\t" + pos.y + "\t" + fullHealth + "\t" + health;
    }
}
