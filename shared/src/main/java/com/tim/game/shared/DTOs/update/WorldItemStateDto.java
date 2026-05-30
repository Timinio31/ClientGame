package com.tim.game.shared.DTOs.update;

import com.tim.game.shared.model.Vector2f;

/**
 * Sichtbares Item in der Welt, z.B. nach Drop oder als Spawn-Loot.
 */
public class WorldItemStateDto {
    private String entityId;
    private ItemStackDto item;
    private int tileX;
    private int tileY;
    private Vector2f position;

    public WorldItemStateDto() {
    }

    public WorldItemStateDto(String entityId, ItemStackDto item, int tileX, int tileY, Vector2f position) {
        this.entityId = entityId;
        this.item = item;
        this.tileX = tileX;
        this.tileY = tileY;
        this.position = position;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public ItemStackDto getItem() {
        return item;
    }

    public void setItem(ItemStackDto item) {
        this.item = item;
    }

    public int getTileX() {
        return tileX;
    }

    public void setTileX(int tileX) {
        this.tileX = tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public void setTileY(int tileY) {
        this.tileY = tileY;
    }

    public Vector2f getPosition() {
        return position;
    }

    public void setPosition(Vector2f position) {
        this.position = position;
    }
}
