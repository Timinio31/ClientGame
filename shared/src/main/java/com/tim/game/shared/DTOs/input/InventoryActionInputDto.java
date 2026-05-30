package com.tim.game.shared.DTOs.input;

/**
 * Client-Input für Inventar-Aktionen.
 * action: SELECT_SLOT, USE_SELECTED, DROP_SELECTED, PICKUP_NEAREST
 */
public class InventoryActionInputDto {
    private String action;
    private int slotIndex = -1;
    private int amount = 1;
    private int targetTileX = -1;
    private int targetTileY = -1;

    public InventoryActionInputDto() {
    }

    public InventoryActionInputDto(String action, int slotIndex, int amount, int targetTileX, int targetTileY) {
        this.action = action;
        this.slotIndex = slotIndex;
        this.amount = amount;
        this.targetTileX = targetTileX;
        this.targetTileY = targetTileY;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getTargetTileX() {
        return targetTileX;
    }

    public void setTargetTileX(int targetTileX) {
        this.targetTileX = targetTileX;
    }

    public int getTargetTileY() {
        return targetTileY;
    }

    public void setTargetTileY(int targetTileY) {
        this.targetTileY = targetTileY;
    }
}
