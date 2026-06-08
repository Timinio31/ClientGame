package com.tim.game.server.world.map;

import com.tim.game.shared.world.BiomeType;
import com.tim.game.shared.world.TileType;

public class MapTile {

    private final int x;
    private final int y;
    private TileType type;
    private boolean walkable;
    private int heightLevel;
    private BiomeType biome;
    private float moisture;
    private float temperature;
    private String resourceType;
    private int resourceAmount;
    private String feature;

    public MapTile(int x, int y, TileType type, boolean walkable) {
        this(x, y, type, walkable, 0, BiomeType.GRASSLAND);
    }

    public MapTile(int x, int y, TileType type, boolean walkable, int heightLevel, BiomeType biome) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.walkable = walkable;
        this.heightLevel = Math.max(0, heightLevel);
        this.biome = biome == null ? BiomeType.GRASSLAND : biome;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
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
        this.heightLevel = Math.max(0, heightLevel);
    }

    public BiomeType getBiome() {
        return biome;
    }

    public void setBiome(BiomeType biome) {
        this.biome = biome == null ? BiomeType.GRASSLAND : biome;
    }

    public float getMoisture() {
        return moisture;
    }

    public void setMoisture(float moisture) {
        this.moisture = moisture;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
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
        this.resourceAmount = Math.max(0, resourceAmount);
        if (this.resourceAmount <= 0) {
            this.resourceType = null;
        }
    }

    public boolean hasResource() {
        return resourceType != null && !resourceType.isBlank() && resourceAmount > 0;
    }

    public boolean consumeResource(int amount) {
        if (!hasResource()) {
            return false;
        }
        resourceAmount = Math.max(0, resourceAmount - Math.max(1, amount));
        if (resourceAmount <= 0) {
            resourceType = null;
        }
        return true;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }
}
