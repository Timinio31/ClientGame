package com.tim.game.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.shared.DTOs.update.BuildingStateDto;
import com.tim.game.shared.DTOs.update.InventorySlotDto;
import com.tim.game.shared.DTOs.update.ItemStackDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.config.WorldSettings;
import com.tim.game.shared.crafting.CraftingCatalog;
import com.tim.game.shared.crafting.CraftingIngredient;
import com.tim.game.shared.crafting.CraftingOutput;
import com.tim.game.shared.crafting.CraftingRecipe;
import com.tim.game.shared.inventory.ItemCatalog;
import com.tim.game.shared.workstation.WorkstationCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Einfache HUD-Ansicht für das erste Crafting-Menü.
 * Die eigentliche Herstellung bleibt serverautoritativ; diese Klasse zeigt nur Rezepte und lokale Hilfsinfos an.
 */
public class CraftingMenuRenderer {

    public void render(ShapeRenderer shapes,
                       SpriteBatch batch,
                       BitmapFont font,
                       OrthographicCamera hudCamera,
                       WorldSnapshotDto snapshot,
                       String clientId,
                       WorldSettings settings,
                       boolean open) {
        if (!open || !isCraftingMenuAllowed(settings)) {
            return;
        }

        float width = Math.min(720f, Gdx.graphics.getWidth() - 80f);
        float height = Math.min(470f, Gdx.graphics.getHeight() - 110f);
        float x = 40f;
        float y = Gdx.graphics.getHeight() - height - 55f;

        PlayerStateDto player = findLocalPlayer(snapshot, clientId);
        List<CraftingRecipe> recipes = visibleRecipes(settings, snapshot, clientId);
        Map<String, Integer> itemCounts = countItems(player);

        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.035f, 0.045f, 0.065f, 0.96f));
        shapes.rect(x, y, width, height);
        shapes.setColor(new Color(0.20f, 0.42f, 0.62f, 1f));
        shapes.rect(x, y + height - 42f, width, 42f);
        shapes.setColor(new Color(0.12f, 0.15f, 0.20f, 0.92f));
        shapes.rect(x + 18f, y + 18f, width - 36f, height - 78f);
        shapes.end();

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(1.15f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Crafting Menu", x + 18f, y + height - 15f);
        font.getData().setScale(0.78f);
        font.setColor(new Color(0.85f, 0.90f, 0.98f, 1f));
        font.draw(batch, "C closes | 1-8 craft a visible recipe | Server validates resources, station and settings", x + 185f, y + height - 17f);

        float lineY = y + height - 74f;
        int index = 0;
        for (CraftingRecipe recipe : recipes) {
            if (index >= 8) {
                break;
            }
            boolean craftableByInventory = hasInputs(itemCounts, recipe);
            font.getData().setScale(0.92f);
            font.setColor(craftableByInventory ? Color.WHITE : new Color(0.62f, 0.66f, 0.72f, 1f));
            font.draw(batch, (index + 1) + ". " + recipe.getDisplayName() + "  [" + recipe.getStationType() + "]", x + 36f, lineY);

            font.getData().setScale(0.72f);
            font.setColor(craftableByInventory ? new Color(0.72f, 0.86f, 0.72f, 1f) : new Color(0.82f, 0.62f, 0.62f, 1f));
            font.draw(batch, craftableByInventory ? "Inputs available" : "Missing inputs or station/context", x + 430f, lineY);

            font.setColor(new Color(0.74f, 0.80f, 0.88f, 1f));
            font.draw(batch, "In:  " + formatInputs(recipe.getInputs(), itemCounts), x + 58f, lineY - 21f);
            font.draw(batch, "Out: " + formatOutputs(recipe.getOutputs()), x + 58f, lineY - 40f);
            if (recipe.getDurationTicks() > 0) {
                font.setColor(new Color(0.90f, 0.78f, 0.52f, 1f));
                font.draw(batch, "Duration prepared: " + recipe.getDurationTicks() + " ticks (currently instant)", x + 430f, lineY - 21f);
            }

            lineY -= 68f;
            index++;
        }

        if (recipes.isEmpty()) {
            font.getData().setScale(0.95f);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "No recipes enabled by the current world settings.", x + 36f, lineY);
        }

        font.getData().setScale(0.72f);
        font.setColor(new Color(0.74f, 0.80f, 0.88f, 1f));
        font.draw(batch, "Note: Workstation recipes already exist in the data model. They require the matching building nearby on the server.", x + 36f, y + 36f);
        batch.end();
    }

    public static boolean isCraftingMenuAllowed(WorldSettings settings) {
        return settings != null
                && settings.isInventoryEnabled()
                && settings.isCraftingEnabled()
                && settings.isCraftingMenuEnabled();
    }

    public static List<CraftingRecipe> visibleRecipes(WorldSettings settings) {
        return visibleRecipes(settings, null, null);
    }

    public static List<CraftingRecipe> visibleRecipes(WorldSettings settings, WorldSnapshotDto snapshot, String clientId) {
        List<CraftingRecipe> result = new ArrayList<>();
        if (!isCraftingMenuAllowed(settings)) {
            return result;
        }
        PlayerStateDto player = findLocalPlayerStatic(snapshot, clientId);
        Map<String, Integer> itemCounts = countItemsStatic(player);
        for (CraftingRecipe recipe : CraftingCatalog.allRecipes()) {
            if (!isRecipeEnabledBySettings(settings, recipe)) {
                continue;
            }
            if (player != null && !hasInputsStatic(itemCounts, recipe)) {
                continue;
            }
            if (player != null && recipe.isWorkstationRecipe() && !hasNearbyStation(snapshot, player, recipe.getStationType())) {
                continue;
            }
            result.add(recipe);
        }
        return result;
    }

    private static boolean isRecipeEnabledBySettings(WorldSettings settings, CraftingRecipe recipe) {
        if (recipe.isInventoryRecipe()) {
            return settings.isInventoryCraftingEnabled();
        }
        if (recipe.isWorkstationRecipe()) {
            return settings.isWorkstationCraftingEnabled();
        }
        return recipe.isToolRecipe() && settings.isToolCraftingEnabled();
    }

    private static boolean hasNearbyStation(WorldSnapshotDto snapshot, PlayerStateDto player, String stationType) {
        if (snapshot == null || snapshot.getBuildings() == null || player == null || player.getPosition() == null) {
            return false;
        }
        int playerX = (int) Math.floor(player.getPosition().getX());
        int playerY = (int) Math.floor(player.getPosition().getY());
        for (BuildingStateDto building : snapshot.getBuildings()) {
            if (building == null) {
                continue;
            }
            if (!WorkstationCatalog.matchesCraftingStation(building.getBuildingType(), stationType)) {
                continue;
            }
            int distance = Math.abs(building.getTileX() - playerX) + Math.abs(building.getTileY() - playerY);
            if (distance <= 1) {
                return true;
            }
        }
        return false;
    }

    private static PlayerStateDto findLocalPlayerStatic(WorldSnapshotDto snapshot, String clientId) {
        if (snapshot == null || snapshot.getPlayers() == null || clientId == null) {
            return null;
        }
        for (PlayerStateDto player : snapshot.getPlayers()) {
            if (player != null && clientId.equals(player.getClientId())) {
                return player;
            }
        }
        return null;
    }

    private static Map<String, Integer> countItemsStatic(PlayerStateDto player) {
        Map<String, Integer> result = new HashMap<>();
        if (player == null || player.getInventorySlots() == null) {
            return result;
        }
        for (InventorySlotDto slot : player.getInventorySlots()) {
            if (slot == null || slot.getItem() == null || slot.getItem().isEmpty()) {
                continue;
            }
            ItemStackDto item = slot.getItem();
            String type = ItemCatalog.normalizeType(item.getItemType());
            result.merge(type, item.getQuantity(), Integer::sum);
        }
        return result;
    }

    private static boolean hasInputsStatic(Map<String, Integer> itemCounts, CraftingRecipe recipe) {
        for (CraftingIngredient ingredient : recipe.getInputs()) {
            if (availableForIngredientStatic(itemCounts, ingredient) < ingredient.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    private static int availableForIngredientStatic(Map<String, Integer> itemCounts, CraftingIngredient ingredient) {
        if (ingredient == null) {
            return 0;
        }
        int total = 0;
        if (ingredient.isCategoryRequirement()) {
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                if (ItemCatalog.hasCategory(entry.getKey(), ingredient.getItemOrCategory())) {
                    total += entry.getValue();
                }
            }
            return total;
        }
        return itemCounts.getOrDefault(ItemCatalog.normalizeType(ingredient.getItemOrCategory()), 0);
    }

    private PlayerStateDto findLocalPlayer(WorldSnapshotDto snapshot, String clientId) {
        if (snapshot == null || snapshot.getPlayers() == null || clientId == null) {
            return null;
        }
        for (PlayerStateDto player : snapshot.getPlayers()) {
            if (player != null && clientId.equals(player.getClientId())) {
                return player;
            }
        }
        return null;
    }

    private Map<String, Integer> countItems(PlayerStateDto player) {
        Map<String, Integer> result = new HashMap<>();
        if (player == null || player.getInventorySlots() == null) {
            return result;
        }
        for (InventorySlotDto slot : player.getInventorySlots()) {
            if (slot == null || slot.getItem() == null || slot.getItem().isEmpty()) {
                continue;
            }
            ItemStackDto item = slot.getItem();
            String type = ItemCatalog.normalizeType(item.getItemType());
            result.merge(type, item.getQuantity(), Integer::sum);
        }
        return result;
    }

    private boolean hasInputs(Map<String, Integer> itemCounts, CraftingRecipe recipe) {
        for (CraftingIngredient ingredient : recipe.getInputs()) {
            if (availableForIngredient(itemCounts, ingredient) < ingredient.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    private int availableForIngredient(Map<String, Integer> itemCounts, CraftingIngredient ingredient) {
        if (ingredient == null) {
            return 0;
        }
        int total = 0;
        if (ingredient.isCategoryRequirement()) {
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                if (ItemCatalog.hasCategory(entry.getKey(), ingredient.getItemOrCategory())) {
                    total += entry.getValue();
                }
            }
            return total;
        }
        return itemCounts.getOrDefault(ItemCatalog.normalizeType(ingredient.getItemOrCategory()), 0);
    }

    private String formatInputs(List<CraftingIngredient> ingredients, Map<String, Integer> itemCounts) {
        if (ingredients == null || ingredients.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        for (CraftingIngredient ingredient : ingredients) {
            String name = ingredient.isCategoryRequirement()
                    ? ItemCatalog.getCategoryDisplayName(ingredient.getItemOrCategory())
                    : ItemCatalog.getDisplayName(ingredient.getItemOrCategory());
            int available = availableForIngredient(itemCounts, ingredient);
            parts.add(ingredient.getQuantity() + "x " + name + " (" + available + ")");
        }
        return String.join(", ", parts);
    }

    private String formatOutputs(List<CraftingOutput> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        for (CraftingOutput output : outputs) {
            String suffix = output.isByproduct() ? " byproduct" : "";
            parts.add(output.getQuantity() + "x " + ItemCatalog.getDisplayName(output.getItemType()) + suffix);
        }
        return String.join(", ", parts);
    }
}
