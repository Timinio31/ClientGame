package com.tim.game.shared.DTOs.update;

public class TileStateDto {

    private int x;
    private int y;
    private String type;
    private boolean walkable;
    private int heightLevel;
    private String biome;
    private String resourceType;
    private int resourceAmount;
    private String feature;

    public TileStateDto() {
    }

    public TileStateDto(int x, int y, String type, boolean walkable) {
        this(x, y, type, walkable, 0, "GRASSLAND", null, 0, null);
    }

    public TileStateDto(int x,
                        int y,
                        String type,
                        boolean walkable,
                        int heightLevel,
                        String biome,
                        String resourceType,
                        int resourceAmount,
                        String feature) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.walkable = walkable;
        this.heightLevel = heightLevel;
        this.biome = biome;
        this.resourceType = resourceType;
        this.resourceAmount = resourceAmount;
        this.feature = feature;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isWalkable() {
        return walkable;
    }

    public void setWalkable(boolean walkable) {
        this.walkable = walkable;
    }

    public int getHeightLevel() {
        return heightLevel;
    }

    public void setHeightLevel(int heightLevel) {
        this.heightLevel = heightLevel;
    }

    public String getBiome() {
        return biome;
    }

    public void setBiome(String biome) {
        this.biome = biome;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getResourceAmount() {
        return resourceAmount;
    }

    public void setResourceAmount(int resourceAmount) {
        this.resourceAmount = resourceAmount;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    @Override
    public String toString() {
        return "TileStateDto{" +
                "x=" + x +
                ", y=" + y +
                ", type='" + type + '\'' +
                ", walkable=" + walkable +
                ", heightLevel=" + heightLevel +
                ", biome='" + biome + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", resourceAmount=" + resourceAmount +
                ", feature='" + feature + '\'' +
                '}';
    }
}
