package com.tim.game.server.world.map.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tim.game.server.world.map.GeneratedMap;
import com.tim.game.server.world.map.MapTile;
import com.tim.game.shared.DTOs.update.TileStateDto;
import com.tim.game.shared.model.Vector2f;
import com.tim.game.shared.world.TileType;
import com.tim.game.shared.world.BiomeType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON based map storage.
 * This is deliberately independent from the procedural generator so future map editor/exporter code
 * can write the same format and the server can load it as a FILE map.
 */
public class SavedMapRepository {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public GeneratedMap load(String roomId, Path file) {
        try {
            SavedMapData data = mapper.readValue(file.toFile(), SavedMapData.class);
            int width = Math.max(8, data.getWidth());
            int height = Math.max(8, data.getHeight());
            float tileSize = data.getTileSize() > 0 ? data.getTileSize() : 1.0f;
            MapTile[][] tiles = new MapTile[width][height];

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                    tiles[x][y] = new MapTile(x, y, border ? TileType.WALL : TileType.GRASS, !border);
                }
            }

            for (TileStateDto tileDto : data.getTiles()) {
                if (tileDto == null || tileDto.getX() < 0 || tileDto.getY() < 0 || tileDto.getX() >= width || tileDto.getY() >= height) {
                    continue;
                }
                TileType type = parseTileType(tileDto.getType());
                MapTile tile = tiles[tileDto.getX()][tileDto.getY()];
                tile.setType(type);
                tile.setWalkable(tileDto.isWalkable());
                tile.setHeightLevel(tileDto.getHeightLevel());
                tile.setBiome(parseBiomeType(tileDto.getBiome()));
                tile.setResourceType(tileDto.getResourceType());
                tile.setResourceAmount(tileDto.getResourceAmount());
                tile.setFeature(tileDto.getFeature());
            }

            GeneratedMap map = new GeneratedMap(roomId, data.getSeed(), width, height, tileSize, tiles);
            for (Vector2f spawnPoint : data.getSpawnPoints()) {
                if (spawnPoint != null) {
                    map.addSpawnPoint(spawnPoint);
                }
            }
            if (map.getSpawnPoints().isEmpty()) {
                map.addSpawnPoint(new Vector2f(tileSize * 1.5f, tileSize * 1.5f));
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Could not load map from " + file, e);
        }
    }

    public void save(GeneratedMap map, Path file, String mapId) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            SavedMapData data = fromGeneratedMap(map, mapId);
            mapper.writeValue(file.toFile(), data);
        } catch (Exception e) {
            throw new RuntimeException("Could not save map to " + file, e);
        }
    }

    public Path defaultMapPath(String mapId) {
        String safe = (mapId == null || mapId.isBlank() ? "default-map" : mapId.trim())
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        return Path.of(System.getProperty("user.home"), ".clientgame", "maps", safe + ".json");
    }

    private SavedMapData fromGeneratedMap(GeneratedMap map, String mapId) {
        SavedMapData data = new SavedMapData();
        data.setMapId(mapId);
        data.setSeed(map.getSeed());
        data.setWidth(map.getWidth());
        data.setHeight(map.getHeight());
        data.setTileSize(map.getTileSize());
        data.setSpawnPoints(new ArrayList<>(map.getSpawnPoints()));

        List<TileStateDto> tileDtos = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                MapTile tile = map.getTile(x, y);
                if (tile == null) {
                    continue;
                }
                tileDtos.add(new TileStateDto(
                        tile.getX(),
                        tile.getY(),
                        tile.getType().name(),
                        tile.isWalkable(),
                        tile.getHeightLevel(),
                        tile.getBiome() == null ? null : tile.getBiome().name(),
                        tile.getResourceType(),
                        tile.getResourceAmount(),
                        tile.getFeature()
                ));
            }
        }
        data.setTiles(tileDtos);
        return data;
    }

    private TileType parseTileType(String raw) {
        if (raw == null || raw.isBlank()) {
            return TileType.GRASS;
        }
        try {
            return TileType.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return TileType.GRASS;
        }
    }

    private BiomeType parseBiomeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return BiomeType.GRASSLAND;
        }
        try {
            return BiomeType.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return BiomeType.GRASSLAND;
        }
    }

    public static class SavedMapData {
        private String mapId = "default-map";
        private long seed;
        private int width = 32;
        private int height = 32;
        private float tileSize = 1.0f;
        private List<TileStateDto> tiles = new ArrayList<>();
        private List<Vector2f> spawnPoints = new ArrayList<>();

        public String getMapId() {
            return mapId;
        }

        public void setMapId(String mapId) {
            this.mapId = mapId;
        }

        public long getSeed() {
            return seed;
        }

        public void setSeed(long seed) {
            this.seed = seed;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public float getTileSize() {
            return tileSize;
        }

        public void setTileSize(float tileSize) {
            this.tileSize = tileSize;
        }

        public List<TileStateDto> getTiles() {
            return tiles;
        }

        public void setTiles(List<TileStateDto> tiles) {
            this.tiles = tiles == null ? new ArrayList<>() : tiles;
        }

        public List<Vector2f> getSpawnPoints() {
            return spawnPoints;
        }

        public void setSpawnPoints(List<Vector2f> spawnPoints) {
            this.spawnPoints = spawnPoints == null ? new ArrayList<>() : spawnPoints;
        }
    }
}
