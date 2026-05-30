package com.tim.game.shared.DTOs.update;

/**
 * Ein Inventar-Slot. item == null bedeutet leer.
 */
public class InventorySlotDto {
    private int slotIndex;
    private ItemStackDto item;
    private boolean selected;

    public InventorySlotDto() {
    }

    public InventorySlotDto(int slotIndex, ItemStackDto item, boolean selected) {
        this.slotIndex = slotIndex;
        this.item = item;
        this.selected = selected;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public ItemStackDto getItem() {
        return item;
    }

    public void setItem(ItemStackDto item) {
        this.item = item;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
