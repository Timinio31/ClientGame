package com.tim.game.shared.DTOs.update;

public class TileStateDto {

    private int x;
    private int y;
    private String type;
    private boolean walkable;

    public TileStateDto() {
    }

    public TileStateDto(int x, int y, String type, boolean walkable) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.walkable = walkable;
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

    @Override
    public String toString() {
        return "TileStateDto{" +
                "x=" + x +
                ", y=" + y +
                ", type='" + type + '\'' +
                ", walkable=" + walkable +
                '}';
    }
}