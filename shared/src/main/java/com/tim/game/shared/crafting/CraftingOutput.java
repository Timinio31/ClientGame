package com.tim.game.shared.crafting;

/**
 * Ein Rezept-Output. Nebenprodukte werden mit byproduct=true markiert.
 */
public class CraftingOutput {
    private String itemType;
    private int quantity = 1;
    private boolean byproduct;

    public CraftingOutput() {
    }

    public CraftingOutput(String itemType, int quantity) {
        this(itemType, quantity, false);
    }

    public CraftingOutput(String itemType, int quantity, boolean byproduct) {
        this.itemType = itemType == null ? "" : itemType.trim().toUpperCase();
        this.quantity = Math.max(1, quantity);
        this.byproduct = byproduct;
    }

    public static CraftingOutput item(String itemType, int quantity) {
        return new CraftingOutput(itemType, quantity, false);
    }

    public static CraftingOutput byproduct(String itemType, int quantity) {
        return new CraftingOutput(itemType, quantity, true);
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType == null ? "" : itemType.trim().toUpperCase();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    public boolean isByproduct() {
        return byproduct;
    }

    public void setByproduct(boolean byproduct) {
        this.byproduct = byproduct;
    }
}
