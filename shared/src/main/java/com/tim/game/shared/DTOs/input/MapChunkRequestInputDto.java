package com.tim.game.shared.DTOs.input;

public class MapChunkRequestInputDto {

    private int minChunkX;
    private int maxChunkX;
    private int minChunkY;
    private int maxChunkY;

    public MapChunkRequestInputDto() {
    }

    public MapChunkRequestInputDto(int minChunkX, int maxChunkX, int minChunkY, int maxChunkY) {
        this.minChunkX = minChunkX;
        this.maxChunkX = maxChunkX;
        this.minChunkY = minChunkY;
        this.maxChunkY = maxChunkY;
    }

    public int getMinChunkX() {
        return minChunkX;
    }

    public void setMinChunkX(int minChunkX) {
        this.minChunkX = minChunkX;
    }

    public int getMaxChunkX() {
        return maxChunkX;
    }

    public void setMaxChunkX(int maxChunkX) {
        this.maxChunkX = maxChunkX;
    }

    public int getMinChunkY() {
        return minChunkY;
    }

    public void setMinChunkY(int minChunkY) {
        this.minChunkY = minChunkY;
    }

    public int getMaxChunkY() {
        return maxChunkY;
    }

    public void setMaxChunkY(int maxChunkY) {
        this.maxChunkY = maxChunkY;
    }
}
