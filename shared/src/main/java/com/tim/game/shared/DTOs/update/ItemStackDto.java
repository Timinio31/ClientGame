package com.tim.game.shared.DTOs.update;

/**
 * Ein stackbares Item mit Anzeige-Metadaten.
 */
public class ItemStackDto {
    private String itemType;
    private String displayName;
    private String description;
    private int quantity;
    private int maxStack;
    private boolean usable;
    private boolean placeable;
    private String buildType;

    public ItemStackDto() {
    }

    public ItemStackDto(String itemType,
                        String displayName,
                        String description,
                        int quantity,
                        int maxStack,
                        boolean usable,
                        boolean placeable,
                        String buildType) {
        this.itemType = itemType;
        this.displayName = displayName;
        this.description = description;
        this.quantity = quantity;
        this.maxStack = maxStack;
        this.usable = usable;
        this.placeable = placeable;
        this.buildType = buildType;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }

    public boolean isPlaceable() {
        return placeable;
    }

    public void setPlaceable(boolean placeable) {
        this.placeable = placeable;
    }

    public String getBuildType() {
        return buildType;
    }

    public void setBuildType(String buildType) {
        this.buildType = buildType;
    }

    public boolean isEmpty() {
        return itemType == null || itemType.isBlank() || quantity <= 0;
    }

    public ItemStackDto copy() {
        return new ItemStackDto(itemType, displayName, description, quantity, maxStack, usable, placeable, buildType);
    }
}
