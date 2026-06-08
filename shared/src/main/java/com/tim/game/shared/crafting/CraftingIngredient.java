package com.tim.game.shared.crafting;

/**
 * Ein benötigter Rezept-Input.
 *
 * type = ITEM bedeutet: itemOrCategory enthält eine konkrete Item-ID.
 * type = CATEGORY bedeutet: itemOrCategory enthält eine Item-Kategorie.
 */
public class CraftingIngredient {
    public static final String TYPE_ITEM = "ITEM";
    public static final String TYPE_CATEGORY = "CATEGORY";

    private String type = TYPE_ITEM;
    private String itemOrCategory;
    private int quantity = 1;

    public CraftingIngredient() {
    }

    public CraftingIngredient(String type, String itemOrCategory, int quantity) {
        this.type = normalizeType(type);
        this.itemOrCategory = itemOrCategory == null ? "" : itemOrCategory.trim().toUpperCase();
        this.quantity = Math.max(1, quantity);
    }

    public static CraftingIngredient item(String itemType, int quantity) {
        return new CraftingIngredient(TYPE_ITEM, itemType, quantity);
    }

    public static CraftingIngredient category(String category, int quantity) {
        return new CraftingIngredient(TYPE_CATEGORY, category, quantity);
    }

    public boolean isCategoryRequirement() {
        return TYPE_CATEGORY.equals(normalizeType(type));
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = normalizeType(type);
    }

    public String getItemOrCategory() {
        return itemOrCategory;
    }

    public void setItemOrCategory(String itemOrCategory) {
        this.itemOrCategory = itemOrCategory == null ? "" : itemOrCategory.trim().toUpperCase();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    private static String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            return TYPE_ITEM;
        }
        String normalized = value.trim().toUpperCase();
        return TYPE_CATEGORY.equals(normalized) ? TYPE_CATEGORY : TYPE_ITEM;
    }
}
