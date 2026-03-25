package com.tim.game.server.world.map;

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

                if (border) {
                    tiles[x][y] = new MapTile(x, y, TileType.WALL, false);
                    continue;
                }

                tiles[x][y] = new MapTile(x, y, TileType.GRASS, true);
            }
        }

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                double roll = random.nextDouble();

                if (roll < 0.08) {
                    tiles[x][y].setType(TileType.WATER);
                    tiles[x][y].setWalkable(false);
                } else if (roll < 0.14) {
                    tiles[x][y].setType(TileType.WALL);
                    tiles[x][y].setWalkable(false);
                }
            }
        }

        int centerX = width / 2;
        int centerY = height / 2;

        clearSpawnArea(tiles, centerX, centerY);

        tiles[centerX][centerY].setType(TileType.SPAWN);
        tiles[centerX][centerY].setWalkable(true);

        GeneratedMap generatedMap = new GeneratedMap(roomId, seed, width, height, tileSize, tiles);
        generatedMap.addSpawnPoint(tileCenter(centerX, centerY, tileSize));

        addExtraSpawnIfWalkable(generatedMap, width / 4, height / 4);
        addExtraSpawnIfWalkable(generatedMap, (width * 3) / 4, height / 4);
        addExtraSpawnIfWalkable(generatedMap, width / 4, (height * 3) / 4);
        addExtraSpawnIfWalkable(generatedMap, (width * 3) / 4, (height * 3) / 4);

        System.out.println("Generated map for room " + roomId + " seed=" + seed
        + " size=" + width + "x" + height + " spawns=" + generatedMap.getSpawnPoints().size());
        System.out.println("Center tile: " + generatedMap.getTile(width / 2, height / 2).getType());

        printMap(generatedMap);

        return generatedMap;
    }

    private void clearSpawnArea(MapTile[][] tiles, int centerX, int centerY) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int y = centerY - 1; y <= centerY + 1; y++) {
                tiles[x][y].setType(TileType.GRASS);
                tiles[x][y].setWalkable(true);
            }
        }
    }

    private void addExtraSpawnIfWalkable(GeneratedMap generatedMap, int tileX, int tileY) {
        MapTile tile = generatedMap.getTile(tileX, tileY);
        if (tile == null) return;

        if (!tile.isWalkable()) {
            tile.setType(TileType.SPAWN);
            tile.setWalkable(true);
        } else {
            tile.setType(TileType.SPAWN);
        }

        generatedMap.addSpawnPoint(tileCenter(tileX, tileY, generatedMap.getTileSize()));
    }

    private Vector2f tileCenter(int tileX, int tileY, float tileSize) {
        float worldX = tileX * tileSize + tileSize * 0.5f;
        float worldY = tileY * tileSize + tileSize * 0.5f;
        return new Vector2f(worldX, worldY);
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