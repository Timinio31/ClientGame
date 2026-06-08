package com.tim.game.shared.DTOs.input;

/**
 * Client -> Server: Spieler möchte ein Rezept herstellen.
 */
public class CraftingInputDto {
    private String recipeId;
    private String context = "INVENTORY";
    private String targetEntityId = "";

    public CraftingInputDto() {
    }

    public CraftingInputDto(String recipeId, String context, String targetEntityId) {
        this.recipeId = recipeId;
        this.context = context;
        this.targetEntityId = targetEntityId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(String targetEntityId) {
        this.targetEntityId = targetEntityId;
    }
}
