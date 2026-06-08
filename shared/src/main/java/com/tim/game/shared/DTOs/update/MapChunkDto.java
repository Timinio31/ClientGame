package com.tim.game.shared.DTOs.update;

import java.util.ArrayList;
import java.util.List;

public class MapChunkDto {

    private String roomId;
    private int chunkX;
    private int chunkY;
    private int chunkSize;
    private int worldWidth;
    private int worldHeight;
    private List<TileStateDto> tiles = new ArrayList<>();

    public MapChunkDto() {
    }

    public MapChunkDto(String roomId, int chunkX, int chunkY, int chunkSize, int worldWidth, int worldHeight) {
        this.roomId = roomId;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkSize = chunkSize;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public void setChunkX(int chunkX) {
        this.chunkX = chunkX;
    }

    public int getChunkY() {
        return chunkY;
    }

    public void setChunkY(int chunkY) {
        this.chunkY = chunkY;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public void setWorldWidth(int worldWidth) {
        this.worldWidth = worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public void setWorldHeight(int worldHeight) {
        this.worldHeight = worldHeight;
    }

    public List<TileStateDto> getTiles() {
        return tiles;
    }

    public void setTiles(List<TileStateDto> tiles) {
        this.tiles = tiles == null ? new ArrayList<>() : tiles;
    }

    public void addTile(TileStateDto tile) {
        if (tile != null) {
            tiles.add(tile);
        }
    }
}
