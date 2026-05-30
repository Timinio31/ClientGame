package com.tim.game.shared.inventory;

import com.tim.game.shared.DTOs.update.ItemStackDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Zentrale Item-Metadaten für Client und Server.
 * Dadurch bleiben Anzeige, Stack-Limits, Nutzbarkeit und Bau-Zuordnung konsistent.
 */
public final class ItemCatalog {
    public static final String WOOD = "WOOD";
    public static final String STONE = "STONE";
    public static final String BERRY = "BERRY";
    public static final String GENERATOR_KIT = "GENERATOR_KIT";

    private ItemCatalog() {
    }

    public static ItemStackDto createStack(String itemType, int quantity) {
        String normalizedType = normalizeType(itemType);
        int maxStack = getMaxStack(normalizedType);
        int normalizedQuantity = Math.max(0, Math.min(quantity, maxStack));
        return new ItemStackDto(
                normalizedType,
                getDisplayName(normalizedType),
                getDescription(normalizedType),
                normalizedQuantity,
                maxStack,
                isUsable(normalizedType),
                isPlaceable(normalizedType),
                getBuildType(normalizedType)
        );
    }

    public static List<ItemStackDto> createStarterStacks() {
        List<ItemStackDto> stacks = new ArrayList<>();
        stacks.add(createStack(GENERATOR_KIT, 3));
        stacks.add(createStack(WOOD, 16));
        stacks.add(createStack(STONE, 10));
        stacks.add(createStack(BERRY, 5));
        return stacks;
    }

    public static String normalizeType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return WOOD;
        }

        return switch (itemType.trim().toUpperCase()) {
            case STONE -> STONE;
            case BERRY -> BERRY;
            case GENERATOR_KIT -> GENERATOR_KIT;
            default -> WOOD;
        };
    }

    public static String getDisplayName(String itemType) {
        return switch (normalizeType(itemType)) {
            case STONE -> "Stone";
            case BERRY -> "Berry";
            case GENERATOR_KIT -> "Generator Kit";
            default -> "Wood";
        };
    }

    public static String getDescription(String itemType) {
        return switch (normalizeType(itemType)) {
            case STONE -> "Basic building material.";
            case BERRY -> "Restores a small amount of health.";
            case GENERATOR_KIT -> "Required to place a generator building.";
            default -> "Basic crafting and building material.";
        };
    }

    public static int getMaxStack(String itemType) {
        return switch (normalizeType(itemType)) {
            case GENERATOR_KIT -> 5;
            default -> 99;
        };
    }

    public static boolean isUsable(String itemType) {
        return BERRY.equals(normalizeType(itemType));
    }

    public static boolean isPlaceable(String itemType) {
        return GENERATOR_KIT.equals(normalizeType(itemType));
    }

    public static String getBuildType(String itemType) {
        return GENERATOR_KIT.equals(normalizeType(itemType)) ? "GENERATOR" : null;
    }

    public static String getRequiredItemForBuilding(String buildingType) {
        if (buildingType == null) {
            return null;
        }
        return switch (buildingType.trim().toUpperCase()) {
            case "GENERATOR" -> GENERATOR_KIT;
            default -> null;
        };
    }
}
