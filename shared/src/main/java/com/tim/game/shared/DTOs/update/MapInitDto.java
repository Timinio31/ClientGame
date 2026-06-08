package com.tim.game.shared.DTOs.update;

import com.tim.game.shared.model.Vector2f;
import com.tim.game.shared.config.WorldSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Einmalige Initialdaten der Map für einen Room.
 * Enthält nur statische Weltinformationen und wird nicht pro Tick übertragen.
 */
public class MapInitDto {

    private String roomId;
    private int width;
    private int height;
    private float tileSize;
    private long seed;
    private int chunkSize = 32;
    private boolean chunkStreamingEnabled = true;
    private WorldSettings worldSettings = WorldSettings.defaults();

    private List<TileStateDto> tiles = new ArrayList<>();
    private List<Vector2f> spawnPoints = new ArrayList<>();

    public MapInitDto() {
    }

    public MapInitDto(String roomId, int width, int height, float tileSize, long seed) {
        this(roomId, width, height, tileSize, seed, WorldSettings.defaults());
    }

    public MapInitDto(String roomId, int width, int height, float tileSize, long seed, WorldSettings worldSettings) {
        this.roomId = roomId;
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.seed = seed;
        setWorldSettings(worldSettings);
        if (this.worldSettings != null) {
            this.chunkSize = this.worldSettings.getChunkSize();
            this.chunkStreamingEnabled = this.worldSettings.isChunkStreamingEnabled();
        }
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public boolean isChunkStreamingEnabled() {
        return chunkStreamingEnabled;
    }

    public void setChunkStreamingEnabled(boolean chunkStreamingEnabled) {
        this.chunkStreamingEnabled = chunkStreamingEnabled;
    }

    public WorldSettings getWorldSettings() {
        return worldSettings;
    }

    public void setWorldSettings(WorldSettings worldSettings) {
        this.worldSettings = worldSettings == null ? WorldSettings.defaults() : worldSettings.normalizedCopy();
        this.chunkSize = this.worldSettings.getChunkSize();
        this.chunkStreamingEnabled = this.worldSettings.isChunkStreamingEnabled();
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

    public void addTile(TileStateDto tile) {
        if (tile != null) {
            tiles.add(tile);
        }
    }

    public void addSpawnPoint(Vector2f spawnPoint) {
        if (spawnPoint != null) {
            spawnPoints.add(spawnPoint);
        }
    }
}
