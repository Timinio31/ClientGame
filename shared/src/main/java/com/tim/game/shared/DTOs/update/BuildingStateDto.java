package com.tim.game.shared.DTOs.update;

import com.tim.game.shared.model.Vector2f;

public class BuildingStateDto {
    private String entityId;
    private String ownerClientId;
    private String buildingType;

    private int tileX;
    private int tileY;

    private Vector2f position; // world position (center of tile)

    public BuildingStateDto() {}

    public BuildingStateDto(String entityId, String ownerClientId, String buildingType,
                            int tileX, int tileY, Vector2f position) {
        this.entityId = entityId;
        this.ownerClientId = ownerClientId;
        this.buildingType = buildingType;
        this.tileX = tileX;
        this.tileY = tileY;
        this.position = position;
    }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getOwnerClientId() { return ownerClientId; }
    public void setOwnerClientId(String ownerClientId) { this.ownerClientId = ownerClientId; }

    public String getBuildingType() { return buildingType; }
    public void setBuildingType(String buildingType) { this.buildingType = buildingType; }

    public int getTileX() { return tileX; }
    public void setTileX(int tileX) { this.tileX = tileX; }

    public int getTileY() { return tileY; }
    public void setTileY(int tileY) { this.tileY = tileY; }

    public Vector2f getPosition() { return position; }
    public void setPosition(Vector2f position) { this.position = position; }

    @Override
    public String toString() {
        return "BuildingStateDto{" +
                "entityId='" + entityId + '\'' +
                ", ownerClientId='" + ownerClientId + '\'' +
                ", buildingType='" + buildingType + '\'' +
                ", tileX=" + tileX +
                ", tileY=" + tileY +
                ", position=" + position +
                '}';
    }
}
