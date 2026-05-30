package com.tim.game.server.world.map;

import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;
import com.tim.game.shared.model.Vector2f;
import com.tim.game.shared.world.TileType;

import java.util.Random;

public class BasicProceduralMapGenerator implements MapGenerator {

    @Override
    public GeneratedMap generate(String roomId, int width, int height, float tileSize, long seed) {
        Random random = new Random(seed);
        MapTile[][] tiles = new MapTile[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                tiles[x][y] = new MapTile(x, y, border ? TileType.WALL : TileType.GRASS, !border);
            }
        }

        paintMountainRanges(random, tiles, width, height);
        paintRiver(random, tiles, width, height);
        paintForestPatches(random, tiles, width, height);
        softenNaturalTransitions(random, tiles, width, height);

        int centerX = width / 2;
        int centerY = height / 2;
        clearArea(tiles, centerX, centerY, 3, TileType.GRASS, true);
        tiles[centerX][centerY].setType(TileType.SPAWN);
        tiles[centerX][centerY].setWalkable(true);

        GeneratedMap generatedMap = new GeneratedMap(roomId, seed, width, height, tileSize, tiles);
        generatedMap.addSpawnPoint(tileCenter(centerX, centerY, tileSize));

        addExtraSpawn(generatedMap, width / 4, height / 4);
        addExtraSpawn(generatedMap, (width * 3) / 4, height / 4);
        addExtraSpawn(generatedMap, width / 4, (height * 3) / 4);
        addExtraSpawn(generatedMap, (width * 3) / 4, (height * 3) / 4);

        DebugConfig.log(DebugCategory.MAP,
                "Generated polished terrain for room " + roomId
                        + " seed=" + seed
                        + " size=" + width + "x" + height
                        + " spawns=" + generatedMap.getSpawnPoints().size());

        if (DebugConfig.isEnabled(DebugCategory.MAP)) {
            printMap(generatedMap);
        }

        return generatedMap;
    }

    private void paintMountainRanges(Random random, MapTile[][] tiles, int width, int height) {
        int ranges = 2 + random.nextInt(2);
        for (int i = 0; i < ranges; i++) {
            int x = 4 + random.nextInt(Math.max(1, width - 8));
            int y = 4 + random.nextInt(Math.max(1, height - 8));
            int steps = 16 + random.nextInt(12);

            for (int step = 0; step < steps; step++) {
                paintBlob(tiles, x, y, 1 + random.nextInt(2), TileType.MOUNTAIN, false);
                x += random.nextInt(3) - 1;
                y += random.nextInt(3) - 1;
                x = clamp(x, 2, width - 3);
                y = clamp(y, 2, height - 3);
            }
        }
    }

    private void paintRiver(Random random, MapTile[][] tiles, int width, int height) {
        boolean horizontal = random.nextBoolean();
        int thickness = 2 + random.nextInt(2);

        if (horizontal) {
            int y = clamp(height / 3 + random.nextInt(Math.max(1, height / 3)), 3, height - 4);
            for (int x = 1; x < width - 1; x++) {
                paintStroke(tiles, x, y, thickness, true, TileType.WATER, false);
                if (random.nextFloat() < 0.55f) {
                    y += random.nextInt(3) - 1;
                    y = clamp(y, 2 + thickness, height - 3 - thickness);
                }
                if (random.nextFloat() < 0.18f) {
                    paintStroke(tiles, x, y + (random.nextBoolean() ? 1 : -1), 1, true, TileType.WATER, false);
                }
            }
        } else {
            int x = clamp(width / 3 + random.nextInt(Math.max(1, width / 3)), 3, width - 4);
            for (int y = 1; y < height - 1; y++) {
                paintStroke(tiles, x, y, thickness, false, TileType.WATER, false);
                if (random.nextFloat() < 0.55f) {
                    x += random.nextInt(3) - 1;
                    x = clamp(x, 2 + thickness, width - 3 - thickness);
                }
                if (random.nextFloat() < 0.18f) {
                    paintStroke(tiles, x + (random.nextBoolean() ? 1 : -1), y, 1, false, TileType.WATER, false);
                }
            }
        }
    }

    private void paintForestPatches(Random random, MapTile[][] tiles, int width, int height) {
        int patches = 9 + random.nextInt(5);
        for (int i = 0; i < patches; i++) {
            int centerX = 3 + random.nextInt(Math.max(1, width - 6));
            int centerY = 3 + random.nextInt(Math.max(1, height - 6));
            int radius = 2 + random.nextInt(3);
            paintBlob(tiles, centerX, centerY, radius, TileType.FOREST, true);
        }
    }

    private void softenNaturalTransitions(Random random, MapTile[][] tiles, int width, int height) {
        for (int pass = 0; pass < 2; pass++) {
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    MapTile tile = tiles[x][y];
                    if (tile.getType() == TileType.GRASS || tile.getType() == TileType.SPAWN) {
                        continue;
                    }

                    int sameNeighbors = countNeighborsOfType(tiles, x, y, tile.getType());
                    if (sameNeighbors <= 1 && random.nextFloat() < 0.70f) {
                        tile.setType(TileType.GRASS);
                        tile.setWalkable(true);
                    }
                }
            }
        }
    }

    private int countNeighborsOfType(MapTile[][] tiles, int x, int y, TileType type) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                MapTile tile = tiles[x + dx][y + dy];
                if (tile.getType() == type) {
                    count++;
                }
            }
        }
        return count;
    }

    private void paintBlob(MapTile[][] tiles, int centerX, int centerY, int radius, TileType type, boolean walkable) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (x <= 0 || y <= 0 || x >= tiles.length - 1 || y >= tiles[0].length - 1) {
                    continue;
                }
                float dx = x - centerX;
                float dy = y - centerY;
                if (dx * dx + dy * dy <= radius * radius + 0.6f) {
                    setNaturalTile(tiles[x][y], type, walkable);
                }
            }
        }
    }

    private void paintStroke(MapTile[][] tiles, int centerX, int centerY, int radius, boolean verticalThickness, TileType type, boolean walkable) {
        for (int offset = -radius; offset <= radius; offset++) {
            int x = verticalThickness ? centerX : centerX + offset;
            int y = verticalThickness ? centerY + offset : centerY;
            if (x <= 0 || y <= 0 || x >= tiles.length - 1 || y >= tiles[0].length - 1) {
                continue;
            }
            setNaturalTile(tiles[x][y], type, walkable);
        }
    }

    private void setNaturalTile(MapTile tile, TileType type, boolean walkable) {
        if (tile.getType() == TileType.WALL || tile.getType() == TileType.SPAWN) {
            return;
        }
        tile.setType(type);
        tile.setWalkable(walkable);
    }

    private void clearArea(MapTile[][] tiles, int centerX, int centerY, int radius, TileType type, boolean walkable) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (x <= 0 || y <= 0 || x >= tiles.length - 1 || y >= tiles[0].length - 1) {
                    continue;
                }
                tiles[x][y].setType(type);
                tiles[x][y].setWalkable(walkable);
            }
        }
    }

    private void addExtraSpawn(GeneratedMap generatedMap, int tileX, int tileY) {
        MapTile tile = generatedMap.getTile(tileX, tileY);
        if (tile == null) {
            return;
        }

        clearArea(generatedMap.getTiles(), tileX, tileY, 1, TileType.GRASS, true);
        tile.setType(TileType.SPAWN);
        tile.setWalkable(true);
        generatedMap.addSpawnPoint(tileCenter(tileX, tileY, generatedMap.getTileSize()));
    }

    private Vector2f tileCenter(int tileX, int tileY, float tileSize) {
        float worldX = tileX * tileSize + tileSize * 0.5f;
        float worldY = tileY * tileSize + tileSize * 0.5f;
        return new Vector2f(worldX, worldY);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String mapToAscii(GeneratedMap map) {
        StringBuilder sb = new StringBuilder();
        for (int y = map.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < map.getWidth(); x++) {
                MapTile tile = map.getTile(x, y);
                if (tile == null) {
                    sb.append('?');
                    continue;
                }
                switch (tile.getType()) {
                    case WALL -> sb.append('#');
                    case GRASS -> sb.append('.');
                    case WATER -> sb.append('~');
                    case SPAWN -> sb.append('S');
                    case FOREST -> sb.append('F');
                    case MOUNTAIN -> sb.append('M');
                    case ROAD -> sb.append('=');
                    default -> sb.append('?');
                }
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static void printMap(GeneratedMap map) {
        System.out.println(mapToAscii(map));
    }
}
