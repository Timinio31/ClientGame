package com.tim.game.server.world.map;

import com.tim.game.shared.model.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class GeneratedMap {

    private final String roomId;
    private final long seed;
    private final int width;
    private final int height;
    private final float tileSize;
    private final MapTile[][] tiles;
    private final List<Vector2f> spawnPoints = new ArrayList<>();

    public GeneratedMap(String roomId, long seed, int width, int height, float tileSize, MapTile[][] tiles) {
        this.roomId = roomId;
        this.seed = seed;
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.tiles = tiles;
    }

    public String getRoomId() {
        return roomId;
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

    public float getTileSize() {
        return tileSize;
    }

    public MapTile[][] getTiles() {
        return tiles;
    }

    public List<Vector2f> getSpawnPoints() {
        return spawnPoints;
    }

    public void addSpawnPoint(Vector2f spawnPoint) {
        this.spawnPoints.add(spawnPoint);
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public MapTile getTile(int x, int y) {
        if (!isInside(x, y)) {
            return null;
        }
        return tiles[x][y];
    }

    public boolean isWalkable(int x, int y) {
        MapTile tile = getTile(x, y);
        return tile != null && tile.isWalkable();
    }
}