package com.tim.game.shared.crafting;

import java.util.ArrayList;
import java.util.List;

/**
 * Datenmodell für Crafting-Rezepte.
 *
 * Es unterstützt bereits:
 * - konkrete Items und Kategorien als Inputs
 * - mehrere Outputs und Nebenprodukte
 * - Inventar-, Workstation- und Werkzeugkontext
 * - optionale Crafting-Dauer
 * - vorbereitete Qualitäts-/Fehlschlag-Flags
 */
public class CraftingRecipe {
    public static final String STATION_INVENTORY = "INVENTORY";
    public static final String STATION_WORKBENCH = "WORKBENCH";
    public static final String STATION_SMITHY = "SMITHY";
    public static final String STATION_COOKING = "COOKING";
    public static final String STATION_MACHINE = "MACHINE";
    public static final String STATION_BUILDING_TOOL = "BUILDING_TOOL";

    private String recipeId;
    private String displayName;
    private String description;
    private String stationType = STATION_INVENTORY;
    private String requiredToolType = "";
    private int durationTicks = 0;
    private boolean qualityEnabled;
    private boolean failureEnabled;
    private boolean unlockedByDefault = true;
    private String resultingBuildType = "";
    private List<CraftingIngredient> inputs = new ArrayList<>();
    private List<CraftingOutput> outputs = new ArrayList<>();

    public CraftingRecipe() {
    }

    public CraftingRecipe(String recipeId,
                          String displayName,
                          String description,
                          String stationType,
                          String requiredToolType,
                          int durationTicks,
                          boolean qualityEnabled,
                          boolean failureEnabled,
                          boolean unlockedByDefault,
                          String resultingBuildType,
                          List<CraftingIngredient> inputs,
                          List<CraftingOutput> outputs) {
        this.recipeId = normalizeId(recipeId);
        this.displayName = displayName == null ? this.recipeId : displayName.trim();
        this.description = description == null ? "" : description.trim();
        this.stationType = normalizeStation(stationType);
        this.requiredToolType = requiredToolType == null ? "" : requiredToolType.trim().toUpperCase();
        this.durationTicks = Math.max(0, durationTicks);
        this.qualityEnabled = qualityEnabled;
        this.failureEnabled = failureEnabled;
        this.unlockedByDefault = unlockedByDefault;
        this.resultingBuildType = resultingBuildType == null ? "" : resultingBuildType.trim().toUpperCase();
        this.inputs = inputs == null ? new ArrayList<>() : new ArrayList<>(inputs);
        this.outputs = outputs == null ? new ArrayList<>() : new ArrayList<>(outputs);
    }

    public boolean isInventoryRecipe() {
        return STATION_INVENTORY.equals(normalizeStation(stationType));
    }

    public boolean isWorkstationRecipe() {
        String normalized = normalizeStation(stationType);
        return STATION_WORKBENCH.equals(normalized)
                || STATION_SMITHY.equals(normalized)
                || STATION_COOKING.equals(normalized)
                || STATION_MACHINE.equals(normalized);
    }

    public boolean isToolRecipe() {
        return STATION_BUILDING_TOOL.equals(normalizeStation(stationType)) || (requiredToolType != null && !requiredToolType.isBlank());
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = normalizeId(recipeId);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? "" : displayName.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = normalizeStation(stationType);
    }

    public String getRequiredToolType() {
        return requiredToolType;
    }

    public void setRequiredToolType(String requiredToolType) {
        this.requiredToolType = requiredToolType == null ? "" : requiredToolType.trim().toUpperCase();
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = Math.max(0, durationTicks);
    }

    public boolean isQualityEnabled() {
        return qualityEnabled;
    }

    public void setQualityEnabled(boolean qualityEnabled) {
        this.qualityEnabled = qualityEnabled;
    }

    public boolean isFailureEnabled() {
        return failureEnabled;
    }

    public void setFailureEnabled(boolean failureEnabled) {
        this.failureEnabled = failureEnabled;
    }

    public boolean isUnlockedByDefault() {
        return unlockedByDefault;
    }

    public void setUnlockedByDefault(boolean unlockedByDefault) {
        this.unlockedByDefault = unlockedByDefault;
    }

    public String getResultingBuildType() {
        return resultingBuildType;
    }

    public void setResultingBuildType(String resultingBuildType) {
        this.resultingBuildType = resultingBuildType == null ? "" : resultingBuildType.trim().toUpperCase();
    }

    public List<CraftingIngredient> getInputs() {
        return inputs;
    }

    public void setInputs(List<CraftingIngredient> inputs) {
        this.inputs = inputs == null ? new ArrayList<>() : inputs;
    }

    public List<CraftingOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<CraftingOutput> outputs) {
        this.outputs = outputs == null ? new ArrayList<>() : outputs;
    }

    private static String normalizeId(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN_RECIPE";
        }
        return value.trim().toUpperCase();
    }

    public static String normalizeStation(String value) {
        if (value == null || value.isBlank()) {
            return STATION_INVENTORY;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case STATION_WORKBENCH, STATION_SMITHY, STATION_COOKING, STATION_MACHINE, STATION_BUILDING_TOOL -> normalized;
            default -> STATION_INVENTORY;
        };
    }
}
