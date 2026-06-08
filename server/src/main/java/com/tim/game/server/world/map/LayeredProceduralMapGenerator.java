package com.tim.game.server.world.map;

import com.tim.game.server.world.map.generation.SeededNoise;
import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;
import com.tim.game.shared.inventory.ItemCatalog;
import com.tim.game.shared.model.Vector2f;
import com.tim.game.shared.world.BiomeType;
import com.tim.game.shared.world.TileType;

import java.util.Random;

public class LayeredProceduralMapGenerator implements MapGenerator {

    private static final float HEIGHT_SCALE = 0.0018f;
    private static final float MOISTURE_SCALE = 0.0024f;
    private static final float TEMPERATURE_SCALE = 0.0019f;
    private static final float DETAIL_SCALE = 0.020f;
    private static final int MAX_HEIGHT_LEVEL = 5;

    @Override
    public GeneratedMap generate(String roomId, int width, int height, float tileSize, long seed) {
        SeededNoise noise = new SeededNoise(seed);
        Random random = new Random(seed ^ 0x6D61704C61796572L);
        MapTile[][] tiles = new MapTile[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = createLayeredTile(x, y, width, height, noise);
            }
        }

        carveRivers(random, noise, tiles, width, height);
        softenBiomeEdges(tiles, width, height);
        guaranteeStarterZone(tiles, width / 2, height / 2, Math.max(8, Math.min(width, height) / 48));

        GeneratedMap generatedMap = new GeneratedMap(roomId, seed, width, height, tileSize, tiles);
        addSpawn(generatedMap, width / 2, height / 2);
        addSpawn(generatedMap, width / 4, height / 4);
        addSpawn(generatedMap, (width * 3) / 4, height / 4);
        addSpawn(generatedMap, width / 4, (height * 3) / 4);
        addSpawn(generatedMap, (width * 3) / 4, (height * 3) / 4);

        DebugConfig.log(DebugCategory.MAP,
                "Generated layered world for room " + roomId
                        + " seed=" + seed
                        + " size=" + width + "x" + height
                        + " spawns=" + generatedMap.getSpawnPoints().size());

        if (DebugConfig.isEnabled(DebugCategory.MAP) && width <= 96 && height <= 96) {
            printMap(generatedMap);
        }

        return generatedMap;
    }

    private MapTile createLayeredTile(int x, int y, int width, int height, SeededNoise noise) {
        boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
        if (border) {
            return new MapTile(x, y, TileType.WALL, false, 0, BiomeType.COAST);
        }

        float heightValue = heightValue(x, y, width, height, noise);
        float moisture = noise.fbm(x, y, MOISTURE_SCALE, 4, 0.52f, 2.0f, 0x201L);
        float temperature = temperatureValue(x, y, height, noise);
        float detail = noise.fbm(x, y, DETAIL_SCALE, 2, 0.45f, 2.0f, 0x401L);
        float cluster = noise.cellular(x, y, Math.max(48f, Math.min(width, height) / 6f), 0x611L);

        int heightLevel = quantizeHeight(heightValue);
        BiomeType biome = pickBiome(heightValue, moisture, temperature, cluster);
        TileType type = tileTypeForBiome(biome, heightValue, moisture, detail);
        boolean walkable = isNaturallyWalkable(type);

        MapTile tile = new MapTile(x, y, type, walkable, heightLevel, biome);
        tile.setMoisture(moisture);
        tile.setTemperature(temperature);
        assignResource(tile, noise, detail, cluster);
        return tile;
    }

    private float heightValue(int x, int y, int width, int height, SeededNoise noise) {
        float base = noise.fbm(x, y, HEIGHT_SCALE, 6, 0.50f, 2.05f, 0x101L);
        float detail = noise.fbm(x, y, HEIGHT_SCALE * 5.2f, 3, 0.45f, 2.0f, 0x131L);
        float continentMask = continentMask(x, y, width, height);
        float value = base * 0.74f + detail * 0.18f + continentMask * 0.18f;
        return clamp01(value);
    }

    private float continentMask(int x, int y, int width, int height) {
        float nx = (x / (float) Math.max(1, width - 1)) * 2f - 1f;
        float ny = (y / (float) Math.max(1, height - 1)) * 2f - 1f;
        float distance = (float) Math.sqrt(nx * nx + ny * ny);
        return clamp01(1.15f - distance);
    }

    private float temperatureValue(int x, int y, int height, SeededNoise noise) {
        float latitude = 1f - Math.abs((y / (float) Math.max(1, height - 1)) * 2f - 1f);
        float noisePart = noise.fbm(x, y, TEMPERATURE_SCALE, 4, 0.50f, 2.0f, 0x301L);
        return clamp01(latitude * 0.58f + noisePart * 0.42f);
    }

    private int quantizeHeight(float heightValue) {
        if (heightValue < 0.25f) {
            return 0;
        }
        if (heightValue < 0.39f) {
            return 1;
        }
        if (heightValue < 0.55f) {
            return 2;
        }
        if (heightValue < 0.70f) {
            return 3;
        }
        if (heightValue < 0.84f) {
            return 4;
        }
        return MAX_HEIGHT_LEVEL;
    }

    private BiomeType pickBiome(float heightValue, float moisture, float temperature, float cluster) {
        if (heightValue < 0.22f) {
            return BiomeType.COAST;
        }
        if (heightValue < 0.27f) {
            return BiomeType.BEACH;
        }
        if (heightValue > 0.78f) {
            return temperature < 0.34f ? BiomeType.SNOW : BiomeType.MOUNTAIN;
        }
        if (temperature < 0.23f) {
            return BiomeType.SNOW;
        }
        if (temperature > 0.72f && moisture < 0.30f) {
            return cluster > 0.74f && heightValue > 0.52f ? BiomeType.MAGMA : BiomeType.DESERT;
        }
        if (moisture > 0.75f) {
            return temperature > 0.40f ? BiomeType.SWAMP : BiomeType.FOREST;
        }
        if (moisture > 0.55f) {
            return BiomeType.FOREST;
        }
        return BiomeType.GRASSLAND;
    }

    private TileType tileTypeForBiome(BiomeType biome, float heightValue, float moisture, float detail) {
        return switch (biome) {
            case COAST, RIVER -> TileType.WATER;
            case BEACH -> TileType.BEACH;
            case DESERT -> TileType.SAND;
            case SWAMP -> TileType.SWAMP;
            case SNOW -> TileType.SNOW;
            case MAGMA -> detail > 0.58f ? TileType.MAGMA : TileType.MOUNTAIN;
            case MOUNTAIN -> heightValue > 0.88f && detail > 0.52f ? TileType.CLIFF : TileType.MOUNTAIN;
            case FOREST -> TileType.FOREST;
            case STARTER, CITY, GRASSLAND -> TileType.GRASS;
        };
    }

    private boolean isNaturallyWalkable(TileType type) {
        return type != TileType.WALL && type != TileType.WATER && type != TileType.MAGMA && type != TileType.CLIFF;
    }

    private void assignResource(MapTile tile, SeededNoise noise, float detail, float cluster) {
        if (!tile.isWalkable()) {
            return;
        }

        float roll = noise.fbm(tile.getX(), tile.getY(), 0.035f, 2, 0.5f, 2.0f, 0xBEEF);
        BiomeType biome = tile.getBiome();

        if (biome == BiomeType.FOREST && roll > 0.62f) {
            tile.setResourceType(ItemCatalog.WOOD);
            tile.setResourceAmount(3 + Math.round(detail * 4f));
        } else if ((biome == BiomeType.GRASSLAND || biome == BiomeType.STARTER) && roll > 0.72f) {
            tile.setResourceType(cluster > 0.55f ? ItemCatalog.BERRY : ItemCatalog.FIBER);
            tile.setResourceAmount(1 + Math.round(detail * 3f));
        } else if ((biome == BiomeType.MOUNTAIN || biome == BiomeType.MAGMA || tile.getType() == TileType.MOUNTAIN) && roll > 0.54f) {
            tile.setResourceType(oreResourceFor(roll, cluster));
            tile.setResourceAmount(2 + Math.round(detail * 5f));
        } else if (biome == BiomeType.DESERT && roll > 0.66f) {
            tile.setResourceType(cluster > 0.78f ? ItemCatalog.RAW_COPPER_ORE : ItemCatalog.SAND);
            tile.setResourceAmount(2 + Math.round(detail * 4f));
        } else if (biome == BiomeType.SWAMP && roll > 0.68f) {
            tile.setResourceType(cluster > 0.50f ? ItemCatalog.CLAY : ItemCatalog.FIBER);
            tile.setResourceAmount(1 + Math.round(detail * 3f));
        } else if (biome == BiomeType.BEACH && roll > 0.70f) {
            tile.setResourceType(ItemCatalog.SAND);
            tile.setResourceAmount(2 + Math.round(detail * 3f));
        }
    }

    private String oreResourceFor(float roll, float cluster) {
        if (cluster > 0.88f) {
            return ItemCatalog.RAW_IRON_ORE;
        }
        if (cluster > 0.74f) {
            return ItemCatalog.RAW_COPPER_ORE;
        }
        if (roll > 0.82f) {
            return ItemCatalog.COAL;
        }
        return ItemCatalog.STONE;
    }

    private void carveRivers(Random random, SeededNoise noise, MapTile[][] tiles, int width, int height) {
        int riverCount = Math.max(2, Math.min(10, (width * height) / 180000));
        for (int i = 0; i < riverCount; i++) {
            int x = 4 + random.nextInt(Math.max(1, width - 8));
            int y = height - 5 - random.nextInt(Math.max(1, Math.max(4, height / 3)));
            int steps = width + height;
            int thickness = 1 + random.nextInt(2);

            for (int step = 0; step < steps; step++) {
                paintRiverTile(tiles, x, y, thickness);
                if (x <= 2 || y <= 2 || x >= width - 3 || y >= height - 3) {
                    break;
                }

                float bestScore = Float.MAX_VALUE;
                int bestX = x;
                int bestY = y - 1;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (Math.abs(dx) + Math.abs(dy) != 1) {
                            continue;
                        }
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx <= 1 || ny <= 1 || nx >= width - 2 || ny >= height - 2) {
                            bestX = nx;
                            bestY = ny;
                            bestScore = -1f;
                            continue;
                        }
                        MapTile candidate = tiles[nx][ny];
                        float meander = noise.randomUnit(nx, ny, 0xCAFE + i) * 0.18f;
                        float coastBias = ny / (float) Math.max(1, height - 1) * 0.08f;
                        float score = candidate.getHeightLevel() + meander + coastBias;
                        if (score < bestScore) {
                            bestScore = score;
                            bestX = nx;
                            bestY = ny;
                        }
                    }
                }
                x = clamp(bestX, 1, width - 2);
                y = clamp(bestY, 1, height - 2);
            }
        }
    }

    private void paintRiverTile(MapTile[][] tiles, int centerX, int centerY, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x <= 0 || y <= 0 || x >= tiles.length - 1 || y >= tiles[0].length - 1) {
                    continue;
                }
                if (dx * dx + dy * dy > radius * radius + 1) {
                    continue;
                }
                MapTile tile = tiles[x][y];
                tile.setType(TileType.WATER);
                tile.setBiome(BiomeType.RIVER);
                tile.setWalkable(false);
                tile.setResourceAmount(0);
                if (tile.getHeightLevel() >= 3) {
                    tile.setFeature("WATERFALL");
                }
            }
        }
    }

    private void softenBiomeEdges(MapTile[][] tiles, int width, int height) {
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                MapTile tile = tiles[x][y];
                if (tile.getType() == TileType.WATER || tile.getType() == TileType.WALL || tile.getType() == TileType.ROAD || tile.getType() == TileType.SPAWN) {
                    continue;
                }
                int same = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        if (tiles[x + dx][y + dy].getBiome() == tile.getBiome()) {
                            same++;
                        }
                    }
                }
                if (same <= 1 && tile.getBiome() != BiomeType.MOUNTAIN && tile.getBiome() != BiomeType.MAGMA) {
                    tile.setBiome(BiomeType.GRASSLAND);
                    tile.setType(TileType.GRASS);
                    tile.setWalkable(true);
                }
            }
        }
    }

    private void guaranteeStarterZone(MapTile[][] tiles, int centerX, int centerY, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (x <= 0 || y <= 0 || x >= tiles.length - 1 || y >= tiles[0].length - 1) {
                    continue;
                }
                MapTile tile = tiles[x][y];
                tile.setType(TileType.GRASS);
                tile.setBiome(BiomeType.STARTER);
                tile.setWalkable(true);
                tile.setHeightLevel(1);
                if ((x + y) % 11 == 0) {
                    tile.setResourceType(ItemCatalog.WOOD);
                    tile.setResourceAmount(4);
                } else if ((x * 3 + y) % 13 == 0) {
                    tile.setResourceType(ItemCatalog.STONE);
                    tile.setResourceAmount(3);
                } else if ((x * 7 + y) % 17 == 0) {
                    tile.setResourceType(ItemCatalog.FIBER);
                    tile.setResourceAmount(2);
                }
            }
        }
        tiles[centerX][centerY].setType(TileType.SPAWN);
        tiles[centerX][centerY].setWalkable(true);
    }

    private void addSpawn(GeneratedMap generatedMap, int tileX, int tileY) {
        MapTile tile = generatedMap.getTile(tileX, tileY);
        if (tile == null) {
            return;
        }
        tile.setType(TileType.SPAWN);
        tile.setBiome(BiomeType.STARTER);
        tile.setWalkable(true);
        generatedMap.addSpawnPoint(tileCenter(tileX, tileY, generatedMap.getTileSize()));
    }

    private Vector2f tileCenter(int tileX, int tileY, float tileSize) {
        float worldX = tileX * tileSize + tileSize * 0.5f;
        float worldY = tileY * tileSize + tileSize * 0.5f;
        return new Vector2f(worldX, worldY);
    }

    private float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
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
                    case SAND, BEACH -> sb.append(':');
                    case SWAMP -> sb.append('W');
                    case SNOW -> sb.append('*');
                    case MAGMA -> sb.append('L');
                    case CLIFF -> sb.append('^');
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
