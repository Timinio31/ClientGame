package com.tim.game.server.world.map;

import com.tim.game.shared.world.TileType;

public class MapTile {

    private final int x;
    private final int y;
    private TileType type;
    private boolean walkable;

    public MapTile(int x, int y, TileType type, boolean walkable) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.walkable = walkable;
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
}