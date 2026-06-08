package com.tim.game.shared.workstation;

import com.tim.game.shared.crafting.CraftingRecipe;

import java.util.Locale;
import java.util.Set;

/**
 * Gemeinsamer Workstation-Katalog für Server-Validierung und Client-Anzeige.
 *
 * Noch keine echte Slot-/Job-Simulation, aber die Typen sind zentralisiert:
 * Workbench, Storage, Terminal, Pumpen, Energiequellen, Öfen, Processor usw.
 */
public final class WorkstationCatalog {
    public static final String WORKBENCH = "WORKBENCH";
    public static final String GENERATOR = "GENERATOR";
    public static final String STORAGE_BOX = "STORAGE_BOX";
    public static final String BIBBLE_TERMINAL = "BIBBLE_TERMINAL";
    public static final String PUMP = "PUMP";
    public static final String LAVA_PUMP = "LAVA_PUMP";
    public static final String OIL_PUMP = "OIL_PUMP";
    public static final String CROP_FARMER = "CROP_FARMER";
    public static final String QUARRY = "QUARRY";
    public static final String WATER_WHEEL = "WATER_WHEEL";
    public static final String WIND_WHEEL = "WIND_WHEEL";
    public static final String FURNACE = "FURNACE";
    public static final String BURNER_FURNACE = "BURNER_FURNACE";
    public static final String HIGH_PERFORMANCE_FURNACE = "HIGH_PERFORMANCE_FURNACE";
    public static final String PROCESSOR = "PROCESSOR";
    public static final String BASIC_CABLE = "BASIC_CABLE";
    public static final String BASIC_PIPE = "BASIC_PIPE";
    public static final String DIRT_BLOCK = "DIRT_BLOCK";
    public static final String WOOD_WALL = "WOOD_WALL";
    public static final String WOOD_FENCE = "WOOD_FENCE";
    public static final String FARMLAND = "FARMLAND";

    private static final Set<String> WORKSTATIONS = Set.of(
            WORKBENCH, GENERATOR, STORAGE_BOX, BIBBLE_TERMINAL, PUMP, LAVA_PUMP, OIL_PUMP,
            CROP_FARMER, QUARRY, WATER_WHEEL, WIND_WHEEL, FURNACE, BURNER_FURNACE,
            HIGH_PERFORMANCE_FURNACE, PROCESSOR
    );

    private static final Set<String> MACHINE_STATIONS = Set.of(
            GENERATOR, FURNACE, BURNER_FURNACE, HIGH_PERFORMANCE_FURNACE, PROCESSOR,
            PUMP, LAVA_PUMP, OIL_PUMP, CROP_FARMER, QUARRY, WATER_WHEEL, WIND_WHEEL
    );

    private WorkstationCatalog() {
    }

    public static String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isWorkstation(String buildingType) {
        return WORKSTATIONS.contains(normalizeType(buildingType));
    }

    public static boolean isNetworkBlock(String buildingType) {
        String type = normalizeType(buildingType);
        return BASIC_CABLE.equals(type) || BASIC_PIPE.equals(type);
    }

    public static boolean isBuildingBlock(String buildingType) {
        String type = normalizeType(buildingType);
        return DIRT_BLOCK.equals(type) || WOOD_WALL.equals(type) || WOOD_FENCE.equals(type) || FARMLAND.equals(type);
    }

    public static boolean matchesCraftingStation(String buildingType, String stationType) {
        String type = normalizeType(buildingType);
        String station = CraftingRecipe.normalizeStation(stationType);
        return switch (station) {
            case CraftingRecipe.STATION_WORKBENCH -> WORKBENCH.equals(type);
            case CraftingRecipe.STATION_SMITHY -> WORKBENCH.equals(type) || PROCESSOR.equals(type);
            case CraftingRecipe.STATION_COOKING -> FURNACE.equals(type) || BURNER_FURNACE.equals(type) || HIGH_PERFORMANCE_FURNACE.equals(type);
            case CraftingRecipe.STATION_MACHINE -> MACHINE_STATIONS.contains(type);
            default -> false;
        };
    }

    public static int defaultMaxHealth(String buildingType) {
        String type = normalizeType(buildingType);
        return switch (type) {
            case WOOD_WALL, WOOD_FENCE -> 60;
            case DIRT_BLOCK, FARMLAND -> 35;
            case BASIC_CABLE, BASIC_PIPE -> 25;
            case STORAGE_BOX -> 80;
            case WORKBENCH -> 100;
            case GENERATOR, BURNER_FURNACE, PROCESSOR -> 140;
            case HIGH_PERFORMANCE_FURNACE -> 220;
            default -> 100;
        };
    }

    public static boolean canAcceptBibbleWorker(String buildingType) {
        String type = normalizeType(buildingType);
        return isWorkstation(type) && !STORAGE_BOX.equals(type) && !BIBBLE_TERMINAL.equals(type);
    }
}
