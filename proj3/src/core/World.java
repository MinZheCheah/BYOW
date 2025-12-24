package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;
import utils.FileUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class World {

    private static final int DEFAULT_WIDTH_MIN = 64;
    private static final int DEFAULT_WIDTH_MAX = 88;
    private static final int DEFAULT_HEIGHT_MIN = 40;
    private static final int DEFAULT_HEIGHT_MAX = 55;
    private static final int DEFAULT_ROOM_SIDE_LENGTH_MIN = 5;
    private static final int DEFAULT_STRUCTURE_DISTANCE_MIN = 1;
    protected static final String SAVE_FILE = "save.txt";


    private int width;
    private int height;
    private int roomSideLengthMin = DEFAULT_ROOM_SIDE_LENGTH_MIN;
    private int roomSideLengthMax;
    private int structureDistanceMin = DEFAULT_STRUCTURE_DISTANCE_MIN;
    private int roomNumMax;
    // assuming average room is 5*5 and takes up at most half of the board area, thus width * height / 98 below
    private int roomNum;

    protected final TETile boundary = Tileset.WALL;
    protected final TETile interior = Tileset.FLOOR;
    protected TETile playerTile = Tileset.PLAYER;
    protected TETile enemyTile = Tileset.ENEMY;
    protected TETile foodTile = Tileset.FOOD;

    private TERenderer ter = new TERenderer();
    protected TETile[][] board;
    protected short[][] boardStatus;
    private long seed;
    private RandomAssumedOnlyGeneratingIntJustForFileSaving random;
    private boolean isGameOver = false;
    private long prevActionTime;

    private Avatar player;
    private Avatar[] enemies;
    private Avatar[] foods;
    private int numOfFoodEatenByPlayer = 0;
    private int numOfFoodEatenByEnemies = 0;


    protected class RandomAssumedOnlyGeneratingIntJustForFileSaving extends Random {
        String seed;
        int numOfCalls;

        public RandomAssumedOnlyGeneratingIntJustForFileSaving(long seed) {
            super(seed);
            this.seed = String.valueOf(seed);
            numOfCalls = 0;
        }

        @Override
        public int nextInt() {
            numOfCalls++;
            return super.nextInt();
        }
        @Override
        public String toString() {
            return seed + "\t" + numOfCalls;
        }
    }

    private class StructRecord {
        int fromX;
        int fromY;
        int toX;
        int toY;
        int grownFromAsHallway;
        ArrayList<Direction> outwardDirections;

        public StructRecord(int fx, int fy, int tx, int ty, int rg) {
            grownFromAsHallway = rg;
            fromX = fx;
            fromY = fy;
            toX = tx;
            toY = ty;
            outwardDirections = new ArrayList<>();
            if (fromX - structureDistanceMin > 2) {
                outwardDirections.add(Direction.LEFT);
            }
            if (fromY - structureDistanceMin > 2) {
                outwardDirections.add(Direction.DOWN);
            }
            if (toX + structureDistanceMin < width - 2) {
                outwardDirections.add(Direction.RIGHT);
            }
            if (toY + structureDistanceMin < height - 2) {
                outwardDirections.add(Direction.UP);
            }
        }

        public StructRecord(int fx, int fy, int tx, int ty) {
            this(fx, fy, tx, ty, -1);
        }

        public static int distX(StructRecord struct1, StructRecord struct2) {
            return Math.max(0, Math.max(struct1.toX, struct2.toX) - Math.min(struct1.fromX, struct2.fromX)
                    - (struct1.toX - struct1.fromX + struct2.toX - struct2.fromX));
        }

        public static int distY(StructRecord struct1, StructRecord struct2) {
            return Math.max(0, Math.max(struct1.toY, struct2.toY) - Math.min(struct1.fromY, struct2.fromY)
                    - (struct1.toY - struct1.fromY + struct2.toY - struct2.fromY));
        }
    }


    /**  Constructors below */
    public World(int w, int h, long inputSeed) {
        width = w;
        height = h;
        seed = inputSeed;
        random = new RandomAssumedOnlyGeneratingIntJustForFileSaving(seed);
        board = new TETile[width][height];
        roomSideLengthMax = Math.max(Math.min(width, height) / 3, roomSideLengthMin);
        roomNumMax = width * height / 98;
        roomNum = 0;
        board = generateBoard();
        getStatusFromBoard(board);
        player = new Avatar(this, 5, Avatar.EntityType.PLAYER, playerTile);
        enemies = new Avatar[(int) (roomNum / 1.5)];
        foods = new Avatar[roomNum];
        for (int i = 0; i < enemies.length; i++) {
            enemies[i] = new Avatar(this, 2, Avatar.EntityType.ENEMY, enemyTile);
        }
        for (int i = 0; i < roomNum; i++) {
            foods[i] = new Avatar(this, 1, Avatar.EntityType.FOOD, foodTile);
        }
        Avatar.drawOnBoard(player, board);
        Avatar.drawOnBoard(enemies, board);
        Avatar.drawOnBoard(foods, board);
        getStatusFromBoard(board);
    }

    public World(long inputSeed, String playerImageFile) {
        seed = inputSeed;
        random = new RandomAssumedOnlyGeneratingIntJustForFileSaving(seed);
        width = random.nextInt(DEFAULT_WIDTH_MIN, DEFAULT_WIDTH_MAX);
        height = random.nextInt(DEFAULT_HEIGHT_MIN, DEFAULT_HEIGHT_MAX);
        //  width = DEFAULT_WIDTH_MAX; height = DEFAULT_HEIGHT_MAX;
        board = new TETile[width][height];
        roomSideLengthMax = Math.max(Math.min(width, height) / 3, roomSideLengthMin);
        roomNumMax = width * height / 98;
        roomNum = 0;
        board = generateBoard();
        getStatusFromBoard(board);
        playerTile = new TETile(playerTile, playerImageFile);
        player = new Avatar(this, 5, Avatar.EntityType.PLAYER, playerTile);
        enemies = new Avatar[(int) (roomNum / 1.5)];
        foods = new Avatar[roomNum];
        for (int i = 0; i < enemies.length; i++) {
            enemies[i] = new Avatar(this, 2, Avatar.EntityType.ENEMY, enemyTile);
        }
        for (int i = 0; i < roomNum; i++) {
            foods[i] = new Avatar(this, 1, Avatar.EntityType.FOOD, foodTile);
        }
        Avatar.drawOnBoard(player, board);
        Avatar.drawOnBoard(enemies, board);
        Avatar.drawOnBoard(foods, board);
        getStatusFromBoard(board);
    }

    public World(long inputSeed) {
        this(inputSeed, null);
    }

    public World(String filename, String playerImageFile) {
        String[] fileContent = FileUtils.readFile(filename).split("\n");
        this.width = Integer.parseInt(fileContent[0]);
        this.height = Integer.parseInt(fileContent[1]);
        String[] lineSplit = fileContent[2].split("\t");
        this.random = new RandomAssumedOnlyGeneratingIntJustForFileSaving(Long.parseLong(lineSplit[0]));
        for (int i = Integer.parseInt(lineSplit[1]); i > 0; i--) {
            random.nextInt();
        }
        this.board = loadBoard(filename);
        getStatusFromBoard(board);
        playerTile = new TETile(playerTile, playerImageFile);
        int lineIndex = height + 3;
        lineSplit = fileContent[lineIndex].split("\t");
        this.roomNum = Integer.parseInt(lineSplit[0]);
        this.numOfFoodEatenByPlayer = Integer.parseInt(lineSplit[1]);
        this.numOfFoodEatenByEnemies = Integer.parseInt(lineSplit[2]);
        lineSplit = fileContent[++lineIndex].split("\t");
        this.player = new Avatar(this, Integer.parseInt(lineSplit[1]), Integer.parseInt(lineSplit[2]),
                Integer.parseInt(lineSplit[3]), Integer.parseInt(lineSplit[4]), Avatar.EntityType.PLAYER, playerTile);
        this.enemies = new Avatar[(int) (roomNum / 1.5)];
        this.foods = new Avatar[roomNum];
        for (int i = 0; i < enemies.length; i++) {
            lineIndex++;
            lineSplit = fileContent[lineIndex].split("\t");
            enemies[i] = new Avatar(this, Integer.parseInt(lineSplit[1]), Integer.parseInt(lineSplit[2]),
                Integer.parseInt(lineSplit[3]), Integer.parseInt(lineSplit[4]), Avatar.EntityType.ENEMY, enemyTile);
            if (enemies[i].getHealth() == 0) {
                enemies[i].die();
            }
        }
        for (int i = 0; i < roomNum; i++) {
            lineIndex++;
            lineSplit = fileContent[lineIndex].split("\t");
            foods[i] = new Avatar(this, Integer.parseInt(lineSplit[1]), Integer.parseInt(lineSplit[2]),
                    Integer.parseInt(lineSplit[3]), Integer.parseInt(lineSplit[4]), Avatar.EntityType.FOOD, foodTile);
            if (foods[i].getHealth() == 0) {
                foods[i].die();
            }
        }
        getStatusFromBoard(board);
    }

    public World(String filename) {
        this(filename, null);
    }

    /*  boardStatus[x][y] being:
        0 : Nothing is here, it's the ambient space.
        1 : Boundary of the map.
        2 : Interior of the map, where entities can stay.
        3 : Player is here.
        4 : There's an enemy here.
        5 : There's a food here.
    */
    private void getStatusFromBoard(TETile[][] tiles) {
        boardStatus = new short[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y].equals(boundary)) {
                    boardStatus[x][y] = 1;
                } else if (tiles[x][y].equals(interior)) {
                    boardStatus[x][y] = 2;
                } else if (tiles[x][y].equals(playerTile)) {
                    boardStatus[x][y] = 3;
                } else if (tiles[x][y].equals(enemyTile)) {
                    boardStatus[x][y] = 4;
                } else if (tiles[x][y].equals(foodTile)) {
                    boardStatus[x][y] = 5;
                } else {
                    boardStatus[x][y] = 0;
                }
            }
        }
    }


    /**  World generating methods below. Til generateBoard()    */
    private void fillTiles(TETile[][] tiles, TETile tileType, int startX, int startY, int fillWidth, int fillHeight) {
        int endX = startX + fillWidth,
            endY = startY + fillHeight;
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                tiles[x][y] = tileType;
            }
        }
    }
    private void fillTiles(TETile[][] tiles, TETile tileType) {
        fillTiles(tiles, tileType, 0, 0, tiles.length, tiles[0].length);
    }

    private void fillTiles2(TETile[][] tiles, TETile tileType, int x1, int y1, int x2, int y2) {
        int fromX = Math.min(x1, x2),
            fromY = Math.min(y1, y2),
            toX = Math.max(x1, x2) + 1,
            toY = Math.max(y1, y2) + 1;
        for (int x = fromX; x < toX; x++) {
            for (int y = fromY; y < toY; y++) {
                tiles[x][y] = tileType;
            }
        }
    }

    private void fillBooleans(boolean[][] booleans, boolean bool, int x1, int y1, int x2, int y2) {
        int fromX = Math.min(x1, x2),
                fromY = Math.min(y1, y2),
                toX = Math.max(x1, x2) + 1,
                toY = Math.max(y1, y2) + 1;
        for (int x = fromX; x < toX; x++) {
            for (int y = fromY; y < toY; y++) {
                booleans[x][y] = bool;
            }
        }
    }

    private void createRoom(TETile[][] tiles, TETile boundaryTileType, TETile interiorTileType,
                            int startX, int startY, int roomWidth, int roomHeight) {
        fillTiles(tiles, boundaryTileType,
                startX, startY, roomWidth, roomHeight);
        fillTiles(tiles, interiorTileType,
                startX + 1, startY + 1, roomWidth - 2, roomHeight - 2);
    }

    private void tryCreateHallway(TETile[][] tiles, boolean[][] roomOccupation, boolean[][] hallwayOccupation,
                                  ArrayList<StructRecord> rooms, ArrayList<StructRecord> hallways,
                                  DisjointSet roomConnection, Random rdm) {
        int fromRoomIndex = Math.floorMod(rdm.nextInt(), rooms.size());
        StructRecord fromRoom = rooms.get(fromRoomIndex);
        Direction direction =
                fromRoom.outwardDirections.get(Math.floorMod(rdm.nextInt(), fromRoom.outwardDirections.size()));
        int hallwayCenterFromX = 0,
            hallwayCenterFromY = 0;
        switch (direction) {
            case RIGHT -> {
                hallwayCenterFromX = fromRoom.toX;
                hallwayCenterFromY = rdm.nextInt(fromRoom.fromY + 1, fromRoom.toY - 1);
            }
            case UP -> {
                hallwayCenterFromX = rdm.nextInt(fromRoom.fromX + 1, fromRoom.toX - 1);
                hallwayCenterFromY = fromRoom.toY;
            }
            case LEFT -> {
                hallwayCenterFromX = fromRoom.fromX - 1;
                hallwayCenterFromY = rdm.nextInt(fromRoom.fromY + 1, fromRoom.toY - 1);
            }
            case DOWN -> {
                hallwayCenterFromX = rdm.nextInt(fromRoom.fromX + 1, fromRoom.toX - 1);
                hallwayCenterFromY = fromRoom.fromY - 1;
            }
            default -> {
                return;
            }
        }
        int currentX = hallwayCenterFromX,
            currentY = hallwayCenterFromY;
        while (true) {
            short growStatus = tryGrowInDirection(currentX, currentY, direction, roomOccupation, hallwayOccupation);
            if (growStatus == 0) {
                return;
            }
            if (growStatus == 1) {
                switch (direction) {
                    case RIGHT -> currentX++;
                    case UP -> currentY++;
                    case LEFT -> currentX--;
                    case DOWN -> currentY--;
                    default -> {
                        return;
                    }
                }
            }
            if (growStatus == 2 || growStatus == 3) {
                hallways.add(growActualHallway(tiles, hallwayOccupation, direction,
                        hallwayCenterFromX, hallwayCenterFromY, currentX, currentY, fromRoomIndex));
                if (growStatus == 2) {
                    int connectedRoomIndex = getIndexOfConnectedRoom(currentX, currentY, rooms);
                    roomConnection.connect(fromRoomIndex, connectedRoomIndex);
                } else {
                    int connectedHallwayIndex = getIndexOfConnectedRoom(currentX, currentY, hallways);
                    roomConnection.connect(fromRoomIndex, hallways.get(connectedHallwayIndex).grownFromAsHallway);
                }
                return;
            }
        }
    }

    /*  return 0 : Can't handle the situation easily
        return 1 : Nothing's in front, just grow normally
        return 2 : There's a room I can grow into in front
        return 3 : There's another hallway in front that I'm intersecting transversally
    */
    private short tryGrowInDirection(int currentCenterX, int currentCenterY, Direction direction,
                                     boolean[][] roomOccupation, boolean[][] hallwayOccupation) {
        boolean[] temp = new boolean[3];
        if (direction == Direction.UP || direction == Direction.DOWN) {
            if ((direction == Direction.UP && currentCenterY == roomOccupation[0].length - 1)
                || (direction == Direction.DOWN && currentCenterY == 0)) {
                return 0;
            }
            for (int i = 0; i < 3; i++) {
                temp[i] = roomOccupation[currentCenterX + i - 1][currentCenterY];
            }
            if (temp[0] && temp[1] && temp[2]) {
                return 2;
            } else if (!temp[0] && !temp[1] && !temp[2]) {
                for (int i = 0; i < 3; i++) {
                    temp[i] = hallwayOccupation[currentCenterX + i - 1][currentCenterY];
                }
                if (temp[0] && temp[1] && temp[2]) {
                    return 3;
                } else if (!temp[0] && !temp[1] && !temp[2]) {
                    return 1;
                }
            }
        } else if (direction == Direction.RIGHT || direction == Direction.LEFT) {
            if ((direction == Direction.RIGHT && currentCenterX == roomOccupation.length - 1)
                || (direction == Direction.LEFT && currentCenterX == 0)) {
                return 0;
            }
            System.arraycopy(roomOccupation[currentCenterX], currentCenterY - 1, temp, 0, 3);
            if (temp[0] && temp[1] && temp[2]) {
                return 2;
            } else if (!temp[0] && !temp[1] && !temp[2]) {
                System.arraycopy(hallwayOccupation[currentCenterX], currentCenterY - 1, temp, 0, 3);
                if (temp[0] && temp[1] && temp[2]) {
                    return 3;
                } else if (!temp[0] && !temp[1] && !temp[2]) {
                    return 1;
                }
            }
        }
        return 0;
    }


    //  The only reason for separating this method from tryCreateHallway() is to pass style check (it's too long)
    private StructRecord growActualHallway(TETile[][] tiles, boolean[][] hallwayOccupation, Direction direction,
                                           int hallwayCenterFromX, int hallwayCenterFromY, int currentX, int currentY,
                                           int fromRoomIndex) {
        int hallwayLength = (direction == Direction.UP || direction == Direction.DOWN)
                ? Math.abs(currentY - hallwayCenterFromY) : Math.abs(currentX - hallwayCenterFromX);
        switch (direction) {
            case RIGHT -> {
                fillTiles(tiles, boundary,
                        hallwayCenterFromX, hallwayCenterFromY - 1, hallwayLength, 3);
                fillTiles(tiles, interior,
                        hallwayCenterFromX - 1, hallwayCenterFromY, hallwayLength + 2, 1);
                for (int x = hallwayCenterFromX; x < currentX; x++) {
                    for (int y = hallwayCenterFromY - 1; y < hallwayCenterFromY + 2; y++) {
                        hallwayOccupation[x][y] = true;
                    }
                }
                return (new StructRecord(hallwayCenterFromX, hallwayCenterFromY - 1,
                        currentX, hallwayCenterFromY + 2, fromRoomIndex));
            }
            case UP -> {
                fillTiles(tiles, boundary,
                        hallwayCenterFromX - 1, hallwayCenterFromY, 3, hallwayLength);
                fillTiles(tiles, interior,
                        hallwayCenterFromX, hallwayCenterFromY - 1, 1, hallwayLength + 2);
                for (int x = hallwayCenterFromX - 1; x < hallwayCenterFromX + 2; x++) {
                    for (int y = hallwayCenterFromY; y < currentY; y++) {
                        hallwayOccupation[x][y] = true;
                    }
                }
                return (new StructRecord(hallwayCenterFromX - 1, hallwayCenterFromY,
                        hallwayCenterFromX + 2, currentY, fromRoomIndex));
            }
            case LEFT -> {
                fillTiles(tiles, boundary,
                        currentX + 1, hallwayCenterFromY - 1, hallwayLength, 3);
                fillTiles(tiles, interior,
                        currentX, hallwayCenterFromY, hallwayLength + 2, 1);
                for (int x = currentX + 1; x < hallwayCenterFromX + 1; x++) {
                    for (int y = hallwayCenterFromY - 1; y < hallwayCenterFromY + 2; y++) {
                        hallwayOccupation[x][y] = true;
                    }
                }
                return (new StructRecord(currentX + 1, hallwayCenterFromY - 1,
                        hallwayCenterFromX + 1, hallwayCenterFromY + 2, fromRoomIndex));
            }
            case DOWN -> {
                fillTiles(tiles, boundary,
                        hallwayCenterFromX - 1, currentY + 1, 3, hallwayLength);
                fillTiles(tiles, interior,
                        hallwayCenterFromX, currentY, 1, hallwayLength + 2);
                for (int x = hallwayCenterFromX - 1; x < hallwayCenterFromX + 2; x++) {
                    for (int y = currentY + 1; y < hallwayCenterFromY + 1; y++) {
                        hallwayOccupation[x][y] = true;
                    }
                }
                return (new StructRecord(hallwayCenterFromX - 1, currentY + 1,
                        hallwayCenterFromX + 2, hallwayCenterFromY + 1, fromRoomIndex));
            }
            default -> {
                return null;
            }
        }
    }

    private int getIndexOfConnectedRoom(int coveredX, int coveredY, ArrayList<StructRecord> rooms) {
        for (int i = 0; i < rooms.size(); i++) {
            StructRecord room = rooms.get(i);
            if (room.fromX <= coveredX && coveredX < room.toX
                && room.fromY <= coveredY && coveredY < room.toY) {
                return i;
            }
        }
        return -1;
    }

    private boolean forceConnect(TETile[][] tiles, StructRecord isolatedRoom, StructRecord helpRoom, int iso, int help,
                                 boolean[][] hallwayOccupation, boolean[][] roomOccupation, ArrayList<StructRecord> h) {
        boolean helpRoomIsLeft = helpRoom.toX < isolatedRoom.toX,
                helpRoomIsBelow = helpRoom.toY < isolatedRoom.toY;
        int isoHallwayX = (isolatedRoom.fromX + isolatedRoom.toX) / 2,
            isoHallwayY = helpRoomIsBelow ? isolatedRoom.fromY - 1 : isolatedRoom.toY,
            helpHallwayX = helpRoomIsLeft ? helpRoom.toX : helpRoom.fromX - 1,
            helpHallwayY = (helpRoom.fromY + helpRoom.toY) / 2,
            auxVertical = helpRoomIsBelow ? -1 : 1,
            auxHorizontal = helpRoomIsLeft ? -1 : 1,
            verFromX = isoHallwayX - 1,
            verFromY = Math.min(isoHallwayY, helpHallwayY + auxVertical),
            verToX = isoHallwayX + 1,
            verToY = Math.max(isoHallwayY, helpHallwayY + auxVertical),
            horFromX = Math.min(helpHallwayX, isoHallwayX - auxHorizontal),
            horFromY = helpHallwayY - 1,
            horToX = Math.max(helpHallwayX, isoHallwayX - auxHorizontal),
            horToY = helpHallwayY + 1;
        if (verFromX < 0 || horFromX < 0 || verFromY < 0 || horFromY < 0
                || verToX >= width || horToX >= width || verToY >= height || horToY >= height) {
            return false;
        }
        for (int x = verFromX; x < verToX; x++) {
            for (int y = verFromY; y < verToY; y++) {
                if (roomOccupation[x][y] || hallwayOccupation[x][y]) {
                    return false;
                }
            }
        }
        for (int x = horFromX; x < horToX; x++) {
            for (int y = horFromY; y < horToY; y++) {
                if (roomOccupation[x][y] || hallwayOccupation[x][y]) {
                    return false;
                }
            }
        }
        h.add(new StructRecord(verFromX, verFromY, verToX + 1, verToY + 1, iso));
        h.add(new StructRecord(horFromX, horFromY, horToX + 1, horToY + 1, help));
        fillTiles2(tiles, boundary, verFromX, verFromY, verToX, verToY);
        fillTiles2(tiles, boundary, horFromX, horFromY, horToX, horToY);
        fillTiles2(tiles, interior, isoHallwayX, isoHallwayY - auxVertical, isoHallwayX, helpHallwayY);
        fillTiles2(tiles, interior, helpHallwayX + auxHorizontal, helpHallwayY, isoHallwayX, helpHallwayY);
        fillBooleans(hallwayOccupation, true, verFromX, verFromY, verToX, verToY);
        fillBooleans(hallwayOccupation, true, horFromX, horFromY, horToX, horToY);
        return true;
    }

    private TETile[][] generateBoard() {
        TETile[][] tileBoard = new TETile[width][height];
        fillTiles(tileBoard, Tileset.NOTHING);
        boolean[][] kindOfFakeRoomOccupation = new boolean[width][height],
                    tileOccupiedByRoom = new boolean[width][height],
                    tileOccupiedByHallway = new boolean[width][height];
        ArrayList<StructRecord> rooms = new ArrayList<>(),
                              hallways = new ArrayList<>();
        loopOfRoomCreatingTries:
        for (int tryNum = 0; roomNum < roomNumMax && tryNum < 2 * roomNumMax; tryNum++) {
            int nextRoomFromX = random.nextInt(width - 4),
                nextRoomFromY = random.nextInt(height - 4),
                nextRoomWidth = random.nextInt(roomSideLengthMin, roomSideLengthMax + 1),
                nextRoomHeight = random.nextInt(roomSideLengthMin, roomSideLengthMax + 1),
                nextRoomToX = nextRoomFromX + nextRoomWidth,
                nextRoomToY = nextRoomFromY + nextRoomHeight;
            if (nextRoomToX >= width || nextRoomToY >= height) {
                continue;
            }
            for (int x = nextRoomFromX; x < nextRoomToX; x++) {
                for (int y = nextRoomFromY; y < nextRoomToY; y++) {
                    if (kindOfFakeRoomOccupation[x][y]) {
                        continue loopOfRoomCreatingTries;
                    }
                }
            }
            createRoom(tileBoard, boundary, interior, nextRoomFromX, nextRoomFromY, nextRoomWidth, nextRoomHeight);
            rooms.add(new StructRecord(nextRoomFromX, nextRoomFromY, nextRoomToX, nextRoomToY));
            for (int x = nextRoomFromX; x < nextRoomToX; x++) {
                for (int y = nextRoomFromY; y < nextRoomToY; y++) {
                    tileOccupiedByRoom[x][y] = true;
                }
            }
            nextRoomFromX -= Math.min(nextRoomFromX, structureDistanceMin);
            nextRoomFromY -= Math.min(nextRoomFromY, structureDistanceMin);
            nextRoomToX += Math.min(width - nextRoomToX - 1, structureDistanceMin);
            nextRoomToY += Math.min(height - nextRoomToY - 1, structureDistanceMin);
            for (int x = nextRoomFromX; x < nextRoomToX; x++) {
                for (int y = nextRoomFromY; y < nextRoomToY; y++) {
                    kindOfFakeRoomOccupation[x][y] = true;
                }
            }
            roomNum++;
        }
        DisjointSet roomConnection = new DisjointSet(roomNum);
        long hallwayTime = System.currentTimeMillis();
        while (roomConnection.getConnenctedComponentNum() > 1 && System.currentTimeMillis() - hallwayTime < 20) {
            tryCreateHallway(tileBoard, tileOccupiedByRoom, tileOccupiedByHallway,
                    rooms, hallways, roomConnection, random);
        }
        fixHallway(tileBoard, roomConnection, rooms, hallways, tileOccupiedByHallway, tileOccupiedByRoom);
        return tileBoard;
    }

    private void fixHallway(TETile[][] tileBoard, DisjointSet roomConnection, ArrayList<StructRecord> rooms,
                            ArrayList<StructRecord> hallways, boolean[][] tileOccupiedByHallway,
                            boolean[][] tileOccupiedByRoom) {
        while (roomConnection.getConnenctedComponentNum() > 1) {
            for (int isoIndex = 0; isoIndex < roomNum; isoIndex++) {
                int compNumAtIndex = roomConnection.componentSize(isoIndex);
                if (compNumAtIndex <= roomNum / 2) {
                    boolean[] marked = new boolean[roomNum];
                    StructRecord isolatedRoom = rooms.get(isoIndex);
                    while (roomConnection.componentSize(isoIndex) == compNumAtIndex) {
                        int helpRoomIndex = -1,
                            minDistX = width;
                        for (int i = 0; i < roomNum; i++) {
                            int dist = StructRecord.distX(isolatedRoom, rooms.get(i));
                            if (!marked[i] && !roomConnection.isConnected(i, isoIndex) && dist < minDistX) {
                                helpRoomIndex = i;
                                minDistX = dist;
                            }
                        }
                        if (helpRoomIndex == -1) {
                            break;
                        }
                        marked[helpRoomIndex] = true;
                        if (forceConnect(tileBoard, isolatedRoom, rooms.get(helpRoomIndex), isoIndex, helpRoomIndex,
                                tileOccupiedByHallway, tileOccupiedByRoom, hallways)) {
                            roomConnection.connect(isoIndex, helpRoomIndex);
                        }
                    }
                }
            }
            for (int isoIndex = 0; isoIndex < roomNum; isoIndex++) {
                int compNumAtIndex = roomConnection.componentSize(isoIndex);
                if (compNumAtIndex <= roomNum / 2) {
                    boolean[] marked = new boolean[roomNum];
                    StructRecord isolatedRoom = rooms.get(isoIndex);
                    while (roomConnection.componentSize(isoIndex) == compNumAtIndex) {
                        int helpRoomIndex = -1,
                            minDistY = height;
                        for (int i = 0; i < roomNum; i++) {
                            int dist = StructRecord.distY(isolatedRoom, rooms.get(i));
                            if (!marked[i] && !roomConnection.isConnected(i, isoIndex) && dist < minDistY) {
                                helpRoomIndex = i;
                                minDistY = dist;
                            }
                        }
                        if (helpRoomIndex == -1) {
                            System.out.println("Seems we've run into some super tricky edge case.\n" + seed);
                            System.exit(114514);
                        }
                        marked[helpRoomIndex] = true;
                        if (forceConnect(tileBoard, rooms.get(helpRoomIndex), isolatedRoom, helpRoomIndex, isoIndex,
                            tileOccupiedByHallway, tileOccupiedByRoom, hallways)) {
                            roomConnection.connect(isoIndex, helpRoomIndex);
                        }
                    }
                }
            }
        }
    }
    /**  World generating methods above. From fillTiles(bla bla)    */


    /**  Screen rendering methods below, Til renderGameInfo()    */
    private void renderScreen() {
        StdDraw.clear(Color.BLACK);
        ter.drawTiles(board);
        renderTilePointed();
        renderGameInfo();
        renderPlayerHealth();
        StdDraw.show();
        ter.resetFont();
    }

    private void renderTilePointed() {
        int tileX = (int) Math.floor(StdDraw.mouseX()),
            tileY = (int) Math.floor(StdDraw.mouseY()) - 1;
        StdDraw.setPenColor(255, 255, 255);
        StdDraw.setFont(new Font("Comic Sans MS", Font.PLAIN, Math.min(width, height) / 2));
        if (-1 < tileX && tileX < width && -1 < tileY && tileY < height) {
            StdDraw.textRight(width, height + 1.5, "Pointing at: " + board[tileX][tileY].description());
        }
    }

    private void renderGameInfo() {
        StdDraw.text(width / 2., height + 1.5, "Get >=75% of the java to win!");
        // font still comic sans here

        StdDraw.setFont(new Font("Calibri", Font.PLAIN, (int) (Math.min(width, height) / 2)));
        StdDraw.textLeft(width + 2, height / 2. + 9, "Total cups of java:  " + roomNum);
        StdDraw.textLeft(width + 2, height / 2. + 7, "You need to get:     " + (int) (Math.ceil(roomNum * .75)));
        StdDraw.textLeft(width + 2, height / 2. + 5, "You've collected:    " + numOfFoodEatenByPlayer);
        StdDraw.textLeft(width + 2, height / 2. + 3, "Enemies have got:  " + numOfFoodEatenByEnemies);

        StdDraw.setPenColor(Color.cyan);
        StdDraw.line(width + .5, 0, width + .5, height + 2);
    }

    private void renderPlayerHealth() {
        StdDraw.setFont(new Font("Arail", Font.PLAIN, Math.min(width, height) / 2));
        int health = player.getHealth();

        double healthX = 0.5;
        double healthY = height + 1.5;

        String hearts = "";
        for (int i = 0; i < health; i++) {
            hearts += "\u2764"; //Unicode for heart symbol"
        }
        StdDraw.setPenColor(StdDraw.RED);
        StdDraw.textLeft(healthX, healthY, hearts);
    }
    /**  Screen rendering methods above, From renderScreen() */


    /**  Game running methods below. Til displayMessageAndExit()    */
    public void runGame() {
        ter.initialize(width + 18, height + 3, 0, 1);
        resetActionTime();
        while (!isGameOver()) {
            updateBoard();
            renderScreen();
            if (((double) numOfFoodEatenByPlayer) / roomNum >= .75) {
                setGameOver(true);
            }
            if (player.isDead() || ((double) numOfFoodEatenByEnemies) / roomNum > .25) {
                setGameOver(false);
            }
        }
    }

    public void updateBoard() {
        if (actionTime() > 750) {
            resetActionTime();
            Avatar.randomMove(enemies);
        }
        if (StdDraw.hasNextKeyTyped()) {
            handleKeyInput(String.valueOf(StdDraw.nextKeyTyped()));
        }
        for (Avatar food: foods) {
            if (Avatar.collide(food, player)) {
                numOfFoodEatenByPlayer++;
            }
            for (Avatar enemy: enemies) {
                if (Avatar.collide(food, enemy)) {
                    numOfFoodEatenByEnemies++;
                }
            }
        }
        for (Avatar enemy: enemies) {
            Avatar.collide(enemy, player);
        }
    }

    protected void handleKeyInput(String input) {
        if (input.equals(":")) {
            while (!StdDraw.hasNextKeyTyped()) {
                continue;
            }
            if (String.valueOf(StdDraw.nextKeyTyped()).equalsIgnoreCase("Q")) {
                saveGame();
                System.exit(114);
            }
        }
        if (input.equals(":Q") || input.equals(":q")) {
            saveGame();
            System.exit(114);
        }
        player.tryMove(input.charAt(0));
    }

    private void resetActionTime() {
        prevActionTime = System.currentTimeMillis();
    }

    private long actionTime() {
        return System.currentTimeMillis() - prevActionTime;
    }

    public void setGameOver(boolean playerWinning) {
        this.isGameOver = true;
        if (!playerWinning) {
            displayMessageAndExit("Game Over!");
        } else {
            displayMessageAndExit("You Win!");
        }
    }

    private void displayMessageAndExit(String displayedMessage) {
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 48));
        StdDraw.text(width / 2.0, height / 2.0, displayedMessage);
        StdDraw.show();
        StdDraw.pause(3000);
        System.exit(0);
    }
    /**  Game running methods above. From runGame()  */


    /**  Saving and loading methods below   */
    public void saveGame() {
        StringBuilder saveString = new StringBuilder(width + "\n" + height + "\n" + random.toString() + "\n");
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                saveString.append(boardStatus[x][y]);
            }
            saveString.append("\n");
        }

        saveString.append(roomNum).append("\t").append(numOfFoodEatenByPlayer).
                append("\t").append(numOfFoodEatenByEnemies).append("\n");
        saveString.append(player.toString()).append("\n");
        for (Avatar enemy: enemies) {
            saveString.append(enemy.toString()).append("\n");
        }
        for (Avatar food: foods) {
            saveString.append(food.toString()).append("\n");
        }

        FileUtils.writeFile(SAVE_FILE, saveString.toString());
    }
    public TETile[][] loadBoard(String filename) {
        String[] fileContent = FileUtils.readFile(filename).split("\n");
        int loadWidth = Integer.parseInt(fileContent[0]),
            loadHeight = Integer.parseInt(fileContent[1]);
        TETile[][] loadedBoard = new TETile[loadWidth][loadHeight];
        for (int i = 0; i < loadHeight; i++) {
            for (int x = 0; x < loadWidth; x++) {
                switch (fileContent[i + 3].charAt(x)) {
                    case '0': {
                        loadedBoard[x][loadHeight - i - 1] = Tileset.NOTHING;
                        break;
                    }
                    case '1': {
                        loadedBoard[x][loadHeight - i - 1] = boundary;
                        break;
                    }
                    case '2': {
                        loadedBoard[x][loadHeight - i - 1] = interior;
                        break;
                    }
                    case '3': {
                        loadedBoard[x][loadHeight - i - 1] = playerTile;
                        break;
                    }
                    case '4': {
                        loadedBoard[x][loadHeight - i - 1] = enemyTile;
                        break;
                    }
                    case '5': {
                        loadedBoard[x][loadHeight - i - 1] = foodTile;
                        break;
                    }
                    default: {
                    }
                }
            }
        }
        return loadedBoard;
    }


    /**  Methods for getting instance variables below.  */
    public boolean isGameOver() {
        return isGameOver;
    }
    public long getSeed() {
        return seed;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    protected RandomAssumedOnlyGeneratingIntJustForFileSaving getRandom() {
        return random;
    }
    public TETile[][] getBoard() {
        return board;
    }
    public short[][] getBoardStatus() {
        return boardStatus;
    }
}
