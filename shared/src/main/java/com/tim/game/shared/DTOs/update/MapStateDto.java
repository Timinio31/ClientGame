package com.tim.game.shared.DTOs.update;

import java.util.ArrayList;
import java.util.List;

public class MapStateDto {

    private String roomId;
    private long seed;
    private int width;
    private int height;
    private float tileSize;

    private List<TileStateDto> tiles = new ArrayList<>();

    public MapStateDto() {
    }

    public MapStateDto(String roomId, long seed, int width, int height, float tileSize) {
        this.roomId = roomId;
        this.seed = seed;
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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
        this.tiles = tiles;
    }

    public void addTile(TileStateDto tile) {
        this.tiles.add(tile);
    }

    @Override
    public String toString() {
        return "MapStateDto{" +
                "roomId='" + roomId + '\'' +
                ", seed=" + seed +
                ", width=" + width +
                ", height=" + height +
                ", tileSize=" + tileSize +
                ", tiles=" + tiles.size() +
                '}';
    }
}