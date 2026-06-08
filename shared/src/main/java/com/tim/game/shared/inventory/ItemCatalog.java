package com.tim.game.shared.inventory;

import com.tim.game.shared.DTOs.update.ItemStackDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Zentrale Item-Metadaten für Client und Server.
 *
 * V1-Polish-Ziel:
 * - Items bleiben aktuell noch Java-basiert, sind aber so strukturiert, dass sie später aus Datenbank/JSON geladen werden können.
 * - Kategorien erlauben flexible Rezepte: Tool-Kopf, Griff, Bindung, Fuel, Food, Capture-Items usw.
 * - Placeables mappen direkt auf BUILDING-/WORKSTATION-Typen.
 */
public final class ItemCatalog {
    public static final String WOOD = "WOOD";
    public static final String STONE = "STONE";
    public static final String BERRY = "BERRY";
    public static final String RED_BERRY = "RED_BERRY";
    public static final String BLUE_BERRY = "BLUE_BERRY";
    public static final String BITTER_BERRY = "BITTER_BERRY";
    public static final String FIBER = "FIBER";
    public static final String GRASS_BUNDLE = "GRASS_BUNDLE";
    public static final String ROPE = "ROPE";
    public static final String PLANK = "PLANK";
    public static final String STICK = "STICK";
    public static final String DIRT = "DIRT";
    public static final String CLAY = "CLAY";
    public static final String SAND = "SAND";
    public static final String COAL = "COAL";
    public static final String RAW_IRON_ORE = "RAW_IRON_ORE";
    public static final String IRON_INGOT = "IRON_INGOT";
    public static final String RAW_COPPER_ORE = "RAW_COPPER_ORE";
    public static final String COPPER_INGOT = "COPPER_INGOT";
    public static final String COPPER_WIRE = "COPPER_WIRE";
    public static final String IRON_PLATE = "IRON_PLATE";
    public static final String PRIMITIVE_TOOL = "PRIMITIVE_TOOL";
    public static final String PICKAXE = "PICKAXE";
    public static final String AXE = "AXE";
    public static final String SHOVEL = "SHOVEL";
    public static final String HAMMER = "HAMMER";
    public static final String WRENCH = "WRENCH";
    public static final String STONE_CLUB = "STONE_CLUB";
    public static final String STONE_SPEAR = "STONE_SPEAR";
    public static final String SIMPLE_BOW = "SIMPLE_BOW";
    public static final String EMPTY_BUCKET = "EMPTY_BUCKET";
    public static final String WATER_BUCKET = "WATER_BUCKET";
    public static final String OIL_BUCKET = "OIL_BUCKET";
    public static final String LAVA_BUCKET = "LAVA_BUCKET";
    public static final String BASIC_CABLE = "BASIC_CABLE";
    public static final String BASIC_PIPE = "BASIC_PIPE";
    public static final String WORKBENCH_KIT = "WORKBENCH_KIT";
    public static final String GENERATOR_KIT = "GENERATOR_KIT";
    public static final String STORAGE_BOX_KIT = "STORAGE_BOX_KIT";
    public static final String BIBBLE_TERMINAL_KIT = "BIBBLE_TERMINAL_KIT";
    public static final String PUMP_KIT = "PUMP_KIT";
    public static final String LAVA_PUMP_KIT = "LAVA_PUMP_KIT";
    public static final String OIL_PUMP_KIT = "OIL_PUMP_KIT";
    public static final String CROP_FARMER_KIT = "CROP_FARMER_KIT";
    public static final String QUARRY_KIT = "QUARRY_KIT";
    public static final String WATER_WHEEL_KIT = "WATER_WHEEL_KIT";
    public static final String WIND_WHEEL_KIT = "WIND_WHEEL_KIT";
    public static final String FURNACE_KIT = "FURNACE_KIT";
    public static final String BURNER_FURNACE_KIT = "BURNER_FURNACE_KIT";
    public static final String HIGH_PERFORMANCE_FURNACE_KIT = "HIGH_PERFORMANCE_FURNACE_KIT";
    public static final String PROCESSOR_KIT = "PROCESSOR_KIT";
    public static final String DIRT_BLOCK_KIT = "DIRT_BLOCK_KIT";
    public static final String WOOD_WALL_KIT = "WOOD_WALL_KIT";
    public static final String WOOD_FENCE_KIT = "WOOD_FENCE_KIT";
    public static final String FARMLAND_KIT = "FARMLAND_KIT";
    public static final String BASIC_CAPTURE_NET = "BASIC_CAPTURE_NET";
    public static final String BASIC_TRAP = "BASIC_TRAP";
    public static final String SLEEP_DART = "SLEEP_DART";
    public static final String BIBBLE_LEASH = "BIBBLE_LEASH";
    public static final String BIBBLE_FOOD = "BIBBLE_FOOD";
    public static final String SIMPLE_MEAL = "SIMPLE_MEAL";

    public static final String CATEGORY_MATERIAL = "MATERIAL";
    public static final String CATEGORY_WOOD_MATERIAL = "WOOD_MATERIAL";
    public static final String CATEGORY_STONE_MATERIAL = "STONE_MATERIAL";
    public static final String CATEGORY_FIBER_MATERIAL = "FIBER_MATERIAL";
    public static final String CATEGORY_ROPE_MATERIAL = "ROPE_MATERIAL";
    public static final String CATEGORY_HANDLE_MATERIAL = "HANDLE_MATERIAL";
    public static final String CATEGORY_TOOL_HEAD_MATERIAL = "TOOL_HEAD_MATERIAL";
    public static final String CATEGORY_BINDING_MATERIAL = "BINDING_MATERIAL";
    public static final String CATEGORY_METAL_BINDING = "METAL_BINDING";
    public static final String CATEGORY_METAL_MATERIAL = "METAL_MATERIAL";
    public static final String CATEGORY_ORE = "ORE";
    public static final String CATEGORY_INGOT = "INGOT";
    public static final String CATEGORY_FUEL = "FUEL";
    public static final String CATEGORY_TOOL = "TOOL";
    public static final String CATEGORY_WEAPON = "WEAPON";
    public static final String CATEGORY_FOOD = "FOOD";
    public static final String CATEGORY_BERRY = "BERRY";
    public static final String CATEGORY_PLACEABLE = "PLACEABLE";
    public static final String CATEGORY_WORKSTATION_KIT = "WORKSTATION_KIT";
    public static final String CATEGORY_BUILDING_BLOCK = "BUILDING_BLOCK";
    public static final String CATEGORY_LIQUID_CONTAINER = "LIQUID_CONTAINER";
    public static final String CATEGORY_CAPTURE_ITEM = "CAPTURE_ITEM";
    public static final String CATEGORY_BIBBLE_ITEM = "BIBBLE_ITEM";
    public static final String CATEGORY_UPGRADE_COMPONENT = "UPGRADE_COMPONENT";
    public static final String CATEGORY_POWER_COMPONENT = "POWER_COMPONENT";
    public static final String CATEGORY_FLUID_COMPONENT = "FLUID_COMPONENT";

    private static final Map<String, ItemDefinition> ITEMS = createDefinitions();

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
        stacks.add(createStack(WOOD, 10));
        stacks.add(createStack(STONE, 8));
        stacks.add(createStack(FIBER, 8));
        stacks.add(createStack(BERRY, 4));
        stacks.add(createStack(STICK, 4));
        stacks.add(createStack(PRIMITIVE_TOOL, 1));
        return stacks;
    }

    public static String normalizeType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            return WOOD;
        }
        String normalized = itemType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BERRIES", "WILD_BERRY" -> BERRY;
            case "BUCKET" -> EMPTY_BUCKET;
            case "IRON_ORE" -> RAW_IRON_ORE;
            case "COPPER_ORE" -> RAW_COPPER_ORE;
            case "FURNACE", "OVEN_KIT" -> FURNACE_KIT;
            case "BIBBLE_TERMINAL" -> BIBBLE_TERMINAL_KIT;
            default -> normalized;
        };
    }

    public static String getDisplayName(String itemType) {
        return definition(itemType).displayName;
    }

    public static String getDescription(String itemType) {
        return definition(itemType).description;
    }

    public static int getMaxStack(String itemType) {
        return definition(itemType).maxStack;
    }

    public static boolean isUsable(String itemType) {
        return definition(itemType).usable;
    }

    public static boolean isPlaceable(String itemType) {
        return definition(itemType).placeable;
    }

    public static String getBuildType(String itemType) {
        String buildType = definition(itemType).buildType;
        return buildType == null || buildType.isBlank() ? null : buildType;
    }

    public static String getRequiredItemForBuilding(String buildingType) {
        if (buildingType == null || buildingType.isBlank()) {
            return null;
        }
        String normalizedBuildingType = buildingType.trim().toUpperCase(Locale.ROOT);
        for (ItemDefinition definition : ITEMS.values()) {
            if (definition.placeable && normalizedBuildingType.equals(definition.buildType)) {
                return definition.itemType;
            }
        }
        return null;
    }

    public static boolean hasCategory(String itemType, String category) {
        if (category == null || category.isBlank()) {
            return false;
        }
        return getCategories(itemType).contains(category.trim().toUpperCase(Locale.ROOT));
    }

    public static Set<String> getCategories(String itemType) {
        return Collections.unmodifiableSet(definition(itemType).categories);
    }

    public static List<String> allItemTypes() {
        return new ArrayList<>(ITEMS.keySet());
    }

    public static List<String> placeableItemTypes() {
        List<String> result = new ArrayList<>();
        for (ItemDefinition definition : ITEMS.values()) {
            if (definition.placeable) {
                result.add(definition.itemType);
            }
        }
        return result;
    }

    public static int getHealAmount(String itemType) {
        return definition(itemType).healAmount;
    }

    public static int getAttackDamage(String itemType) {
        return definition(itemType).attackDamage;
    }

    public static int getToolPower(String itemType) {
        return definition(itemType).toolPower;
    }

    public static float getCaptureModifier(String itemType) {
        return definition(itemType).captureModifier;
    }

    public static boolean isWeapon(String itemType) {
        return hasCategory(itemType, CATEGORY_WEAPON);
    }

    public static boolean isTool(String itemType) {
        return hasCategory(itemType, CATEGORY_TOOL);
    }

    public static boolean isCaptureItem(String itemType) {
        return hasCategory(itemType, CATEGORY_CAPTURE_ITEM);
    }

    public static String getCategoryDisplayName(String category) {
        if (category == null || category.isBlank()) {
            return "Unknown Category";
        }
        return switch (category.trim().toUpperCase(Locale.ROOT)) {
            case CATEGORY_MATERIAL -> "Any material";
            case CATEGORY_WOOD_MATERIAL -> "Wood material";
            case CATEGORY_STONE_MATERIAL -> "Stone material";
            case CATEGORY_FIBER_MATERIAL -> "Fiber material";
            case CATEGORY_ROPE_MATERIAL -> "Rope/binding material";
            case CATEGORY_HANDLE_MATERIAL -> "Handle material";
            case CATEGORY_TOOL_HEAD_MATERIAL -> "Tool-head material";
            case CATEGORY_BINDING_MATERIAL -> "Binding material";
            case CATEGORY_METAL_BINDING -> "Metal binding";
            case CATEGORY_METAL_MATERIAL -> "Metal material";
            case CATEGORY_ORE -> "Ore";
            case CATEGORY_INGOT -> "Ingot";
            case CATEGORY_FUEL -> "Fuel";
            case CATEGORY_TOOL -> "Tool";
            case CATEGORY_WEAPON -> "Weapon";
            case CATEGORY_FOOD -> "Food";
            case CATEGORY_BERRY -> "Berry";
            case CATEGORY_PLACEABLE -> "Placeable";
            case CATEGORY_WORKSTATION_KIT -> "Workstation kit";
            case CATEGORY_BUILDING_BLOCK -> "Building block";
            case CATEGORY_LIQUID_CONTAINER -> "Liquid container";
            case CATEGORY_CAPTURE_ITEM -> "Capture item";
            case CATEGORY_BIBBLE_ITEM -> "Bibble item";
            case CATEGORY_UPGRADE_COMPONENT -> "Upgrade component";
            case CATEGORY_POWER_COMPONENT -> "Power component";
            case CATEGORY_FLUID_COMPONENT -> "Fluid component";
            default -> prettify(category);
        };
    }

    private static ItemDefinition definition(String itemType) {
        String normalized = normalizeType(itemType);
        ItemDefinition known = ITEMS.get(normalized);
        if (known != null) {
            return known;
        }
        return new ItemDefinition(normalized, prettify(normalized), "Unregistered item placeholder. Add it to ItemCatalog or the future item database.", 99, false, false, null, 0, 1, 1, 0f, Set.of(CATEGORY_MATERIAL));
    }

    private static Map<String, ItemDefinition> createDefinitions() {
        Map<String, ItemDefinition> map = new LinkedHashMap<>();

        define(map, WOOD, "Wood", "Basic crafting and building material. Can be used as handle material and weak fuel.", 99, false, false, null, 0, 2, 1, 0f, CATEGORY_MATERIAL, CATEGORY_WOOD_MATERIAL, CATEGORY_HANDLE_MATERIAL, CATEGORY_FUEL);
        define(map, STONE, "Stone", "Basic stone material for tools, buildings and machines.", 99, false, false, null, 0, 3, 2, 0f, CATEGORY_MATERIAL, CATEGORY_STONE_MATERIAL, CATEGORY_TOOL_HEAD_MATERIAL);
        define(map, BERRY, "Wild Berry", "Basic food. Restores a small amount of health and can feed Bibbles.", 20, true, false, null, 15, 1, 1, 0f, CATEGORY_FOOD, CATEGORY_BERRY, CATEGORY_BIBBLE_ITEM);
        define(map, RED_BERRY, "Red Berry", "Sweet berry with stronger healing. Later usable for brewing.", 20, true, false, null, 22, 1, 1, 0f, CATEGORY_FOOD, CATEGORY_BERRY, CATEGORY_BIBBLE_ITEM);
        define(map, BLUE_BERRY, "Blue Berry", "Calming berry. Later useful for capture and brewing.", 20, true, false, null, 10, 1, 1, 4f, CATEGORY_FOOD, CATEGORY_BERRY, CATEGORY_BIBBLE_ITEM);
        define(map, BITTER_BERRY, "Bitter Berry", "Risky berry with weak healing. Later may create negative brewing effects.", 20, true, false, null, 5, 1, 1, 0f, CATEGORY_FOOD, CATEGORY_BERRY, CATEGORY_BIBBLE_ITEM);
        define(map, FIBER, "Plant Fiber", "Flexible natural material used for ropes and basic equipment.", 99, false, false, null, 0, 1, 1, 0f, CATEGORY_MATERIAL, CATEGORY_FIBER_MATERIAL, CATEGORY_ROPE_MATERIAL, CATEGORY_BINDING_MATERIAL);
        define(map, GRASS_BUNDLE, "Grass Bundle", "Cheap binding material. Later recipes can reduce durability when this replaces rope.", 99, false, false, null, 0, 1, 1, 0f, CATEGORY_MATERIAL, CATEGORY_FIBER_MATERIAL, CATEGORY_BINDING_MATERIAL);
        define(map, ROPE, "Simple Rope", "Basic rope material for tools, construction kits and Bibble handling.", 99, false, false, null, 0, 1, 1, 0f, CATEGORY_MATERIAL, CATEGORY_ROPE_MATERIAL, CATEGORY_BINDING_MATERIAL, CATEGORY_BIBBLE_ITEM);
        define(map, PLANK, "Wooden Plank", "Prepared wooden component for crafting and construction.", 99, false, false, null, 0, 2, 1, 0f, CATEGORY_MATERIAL, CATEGORY_WOOD_MATERIAL, "PREPARED_COMPONENT");
        define(map, STICK, "Stick", "Simple handle component and weak weapon material.", 99, false, false, null, 0, 3, 1, 0f, CATEGORY_MATERIAL, CATEGORY_WOOD_MATERIAL, CATEGORY_HANDLE_MATERIAL);
        define(map, DIRT, "Dirt", "Terrain and farming material.", 99, false, false, null, 0, 1, 1, 0f, CATEGORY_MATERIAL, CATEGORY_BUILDING_BLOCK);
        define(map, CLAY, "Clay", "Early furnace and pipe material.", 99, false, false, null, 0, 1, 1, 0f, CATEGORY_MATERIAL, CATEGORY_BUILDING_BLOCK);
        define(map, SAND, "Sand", "Raw material for later glass and processing chains.", 99, false, false, null, 0, 1, 1, 0f, CATEGORY_MATERIAL, CATEGORY_BUILDING_BLOCK);
        define(map, COAL, "Coal", "Simple fuel for furnaces and generators.", 99, false, false, null, 0, 1, 1, 0f, CATEGORY_MATERIAL, CATEGORY_FUEL);
        define(map, RAW_IRON_ORE, "Raw Iron Ore", "Unprocessed iron ore. Needs furnace processing.", 99, false, false, null, 0, 2, 1, 0f, CATEGORY_MATERIAL, CATEGORY_ORE, CATEGORY_STONE_MATERIAL);
        define(map, IRON_INGOT, "Iron Ingot", "Strong metal material for robust tools, upgrades and machines.", 99, false, false, null, 0, 4, 4, 0f, CATEGORY_MATERIAL, CATEGORY_INGOT, CATEGORY_METAL_MATERIAL, CATEGORY_METAL_BINDING, CATEGORY_TOOL_HEAD_MATERIAL, CATEGORY_UPGRADE_COMPONENT);
        define(map, RAW_COPPER_ORE, "Raw Copper Ore", "Unprocessed copper ore. Needed for wires and basic power systems.", 99, false, false, null, 0, 2, 1, 0f, CATEGORY_MATERIAL, CATEGORY_ORE, CATEGORY_STONE_MATERIAL);
        define(map, COPPER_INGOT, "Copper Ingot", "Conductive metal for cables, pumps and generator upgrades.", 99, false, false, null, 0, 3, 3, 0f, CATEGORY_MATERIAL, CATEGORY_INGOT, CATEGORY_METAL_MATERIAL, CATEGORY_POWER_COMPONENT, CATEGORY_UPGRADE_COMPONENT);
        define(map, COPPER_WIRE, "Copper Wire", "Basic wiring component.", 99, false, false, null, 0, 1, 1, 0f, CATEGORY_MATERIAL, CATEGORY_POWER_COMPONENT, CATEGORY_UPGRADE_COMPONENT);
        define(map, IRON_PLATE, "Iron Plate", "Machine casing and upgrade component.", 99, false, false, null, 0, 3, 3, 0f, CATEGORY_MATERIAL, CATEGORY_METAL_MATERIAL, CATEGORY_UPGRADE_COMPONENT);

        define(map, PRIMITIVE_TOOL, "Primitive Tool", "Basic placeholder tool for early crafting and harvesting.", 1, false, false, null, 0, 5, 2, 0f, CATEGORY_TOOL, CATEGORY_WEAPON);
        define(map, PICKAXE, "Stone Pickaxe", "Basic pickaxe for stone and ore harvesting.", 1, false, false, null, 0, 7, 4, 0f, CATEGORY_TOOL, CATEGORY_WEAPON);
        define(map, AXE, "Stone Axe", "Basic axe for wood harvesting and emergency combat.", 1, false, false, null, 0, 8, 3, 0f, CATEGORY_TOOL, CATEGORY_WEAPON);
        define(map, SHOVEL, "Stone Shovel", "Basic shovel for dirt, clay and sand.", 1, false, false, null, 0, 5, 3, 0f, CATEGORY_TOOL, CATEGORY_WEAPON);
        define(map, HAMMER, "Hammer", "Building and later construction-upgrade tool.", 1, false, false, null, 0, 6, 2, 0f, CATEGORY_TOOL, CATEGORY_WEAPON);
        define(map, WRENCH, "Wrench", "Utility tool for machines, pipes, cables and upgrades.", 1, false, false, null, 0, 4, 2, 0f, CATEGORY_TOOL);
        define(map, STONE_CLUB, "Stone Club", "Cheap early weapon with better melee damage than fists.", 1, false, false, null, 0, 11, 1, 0f, CATEGORY_WEAPON);
        define(map, STONE_SPEAR, "Stone Spear", "Early weapon with higher damage and later throw support.", 1, false, false, null, 0, 13, 1, 0f, CATEGORY_WEAPON);
        define(map, SIMPLE_BOW, "Simple Bow", "Prepared ranged weapon placeholder. Arrows/projectiles come later.", 1, false, false, null, 0, 9, 1, 0f, CATEGORY_WEAPON);

        define(map, EMPTY_BUCKET, "Empty Bucket", "Container for water, lava, oil and other liquids.", 16, false, false, null, 0, 1, 1, 0f, CATEGORY_TOOL, CATEGORY_LIQUID_CONTAINER, CATEGORY_FLUID_COMPONENT);
        define(map, WATER_BUCKET, "Water Bucket", "Bucket filled with water. Usable for farming, crafting and water machines.", 1, true, false, null, 0, 1, 1, 0f, CATEGORY_LIQUID_CONTAINER, CATEGORY_FLUID_COMPONENT);
        define(map, OIL_BUCKET, "Oil Bucket", "Bucket filled with oil for later power chains.", 1, false, false, null, 0, 1, 1, 0f, CATEGORY_LIQUID_CONTAINER, CATEGORY_FLUID_COMPONENT, CATEGORY_FUEL);
        define(map, LAVA_BUCKET, "Lava Bucket", "Dangerous high-energy fluid for later furnace chains.", 1, false, false, null, 0, 1, 1, 0f, CATEGORY_LIQUID_CONTAINER, CATEGORY_FLUID_COMPONENT, CATEGORY_FUEL);
        define(map, BASIC_CABLE, "Basic Cable", "Connects generators and consumers.", 99, false, true, "BASIC_CABLE", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_POWER_COMPONENT);
        define(map, BASIC_PIPE, "Basic Pipe", "Connects pumps, tanks and liquid consumers.", 99, false, true, "BASIC_PIPE", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_FLUID_COMPONENT);

        define(map, WORKBENCH_KIT, "Workbench Kit", "Placeable kit for a workbench building.", 5, false, true, "WORKBENCH", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT);
        define(map, GENERATOR_KIT, "Generator Kit", "Placeable generator core. Needs a nearby power source or cable network later.", 5, false, true, "GENERATOR", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT, CATEGORY_POWER_COMPONENT);
        define(map, STORAGE_BOX_KIT, "Storage Box Kit", "Placeable storage container.", 5, false, true, "STORAGE_BOX", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT);
        define(map, BIBBLE_TERMINAL_KIT, "Bibble Terminal Kit", "Placeable terminal for global Bibble storage access.", 5, false, true, "BIBBLE_TERMINAL", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT, CATEGORY_BIBBLE_ITEM);
        define(map, PUMP_KIT, "Pump Kit", "Generic liquid pump. Upgradeable into specialized variants.", 5, false, true, "PUMP", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT, CATEGORY_FLUID_COMPONENT);
        define(map, LAVA_PUMP_KIT, "Lava Pump Kit", "Specialized pump for lava networks.", 5, false, true, "LAVA_PUMP", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT, CATEGORY_FLUID_COMPONENT);
        define(map, OIL_PUMP_KIT, "Oil Pump Kit", "Specialized pump for oil networks.", 5, false, true, "OIL_PUMP", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT, CATEGORY_FLUID_COMPONENT);
        define(map, CROP_FARMER_KIT, "Crop Farmer Kit", "Automated crop workstation placeholder.", 5, false, true, "CROP_FARMER", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT);
        define(map, QUARRY_KIT, "Quarry Kit", "Automated stone and ore workstation placeholder.", 5, false, true, "QUARRY", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT);
        define(map, WATER_WHEEL_KIT, "Water Wheel Kit", "Water-based power source.", 5, false, true, "WATER_WHEEL", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT, CATEGORY_POWER_COMPONENT);
        define(map, WIND_WHEEL_KIT, "Wind Wheel Kit", "Wind-based power source.", 5, false, true, "WIND_WHEEL", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT, CATEGORY_POWER_COMPONENT);
        define(map, FURNACE_KIT, "Furnace Kit", "Basic furnace for ore processing and cooked food.", 5, false, true, "FURNACE", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT);
        define(map, BURNER_FURNACE_KIT, "Burner Furnace Kit", "Fuel-driven furnace power source and processor.", 5, false, true, "BURNER_FURNACE", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT, CATEGORY_POWER_COMPONENT);
        define(map, HIGH_PERFORMANCE_FURNACE_KIT, "High Performance Furnace Kit", "Advanced furnace placeholder for later tech tiers.", 5, false, true, "HIGH_PERFORMANCE_FURNACE", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT);
        define(map, PROCESSOR_KIT, "Processor Kit", "Basic material processor for wires, plates and advanced components.", 5, false, true, "PROCESSOR", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_WORKSTATION_KIT);
        define(map, DIRT_BLOCK_KIT, "Dirt Block", "Placeable dirt block / terrain marker.", 99, false, true, "DIRT_BLOCK", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_BUILDING_BLOCK);
        define(map, WOOD_WALL_KIT, "Wood Wall", "Simple wall segment for houses and fenced areas.", 99, false, true, "WOOD_WALL", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_BUILDING_BLOCK);
        define(map, WOOD_FENCE_KIT, "Wood Fence", "Simple fence segment for Bibble areas.", 99, false, true, "WOOD_FENCE", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_BUILDING_BLOCK);
        define(map, FARMLAND_KIT, "Farmland Tile", "Placeable farm-land marker for early crop systems.", 99, false, true, "FARMLAND", 0, 1, 1, 0f, CATEGORY_PLACEABLE, CATEGORY_BUILDING_BLOCK);

        define(map, BASIC_CAPTURE_NET, "Basic Capture Net", "Early Bibble capture item. Small capture modifier.", 16, true, false, null, 0, 1, 1, 10f, CATEGORY_CAPTURE_ITEM, CATEGORY_BIBBLE_ITEM);
        define(map, BASIC_TRAP, "Basic Trap", "Placed later for capture setups. Currently acts as capture item.", 16, true, false, null, 0, 1, 1, 16f, CATEGORY_CAPTURE_ITEM, CATEGORY_BIBBLE_ITEM);
        define(map, SLEEP_DART, "Sleep Dart", "Future projectile item for sleep/paralysis capture bonuses.", 32, true, false, null, 0, 2, 1, 22f, CATEGORY_CAPTURE_ITEM, CATEGORY_WEAPON, CATEGORY_BIBBLE_ITEM);
        define(map, BIBBLE_LEASH, "Bibble Leash", "Control/training item for Bibbles and fenced areas.", 16, true, false, null, 0, 1, 1, 3f, CATEGORY_BIBBLE_ITEM, CATEGORY_BINDING_MATERIAL);
        define(map, BIBBLE_FOOD, "Bibble Food", "Prepared food for training, connection and work speed systems.", 32, true, false, null, 8, 1, 1, 5f, CATEGORY_FOOD, CATEGORY_BIBBLE_ITEM);
        define(map, SIMPLE_MEAL, "Simple Meal", "Early cooked food with reliable healing.", 16, true, false, null, 35, 1, 1, 0f, CATEGORY_FOOD);

        return map;
    }

    private static void define(Map<String, ItemDefinition> map,
                               String itemType,
                               String displayName,
                               String description,
                               int maxStack,
                               boolean usable,
                               boolean placeable,
                               String buildType,
                               int healAmount,
                               int attackDamage,
                               int toolPower,
                               float captureModifier,
                               String... categories) {
        Set<String> normalizedCategories = new LinkedHashSet<>();
        if (categories != null) {
            for (String category : categories) {
                if (category != null && !category.isBlank()) {
                    normalizedCategories.add(category.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        if (normalizedCategories.isEmpty()) {
            normalizedCategories.add(CATEGORY_MATERIAL);
        }
        String normalizedType = normalizeType(itemType);
        map.put(normalizedType, new ItemDefinition(
                normalizedType,
                displayName,
                description,
                Math.max(1, maxStack),
                usable,
                placeable,
                buildType == null ? null : buildType.trim().toUpperCase(Locale.ROOT),
                Math.max(0, healAmount),
                Math.max(0, attackDamage),
                Math.max(0, toolPower),
                captureModifier,
                normalizedCategories
        ));
    }

    private static String prettify(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        String[] parts = value.trim().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private static final class ItemDefinition {
        private final String itemType;
        private final String displayName;
        private final String description;
        private final int maxStack;
        private final boolean usable;
        private final boolean placeable;
        private final String buildType;
        private final int healAmount;
        private final int attackDamage;
        private final int toolPower;
        private final float captureModifier;
        private final Set<String> categories;

        private ItemDefinition(String itemType,
                               String displayName,
                               String description,
                               int maxStack,
                               boolean usable,
                               boolean placeable,
                               String buildType,
                               int healAmount,
                               int attackDamage,
                               int toolPower,
                               float captureModifier,
                               Set<String> categories) {
            this.itemType = itemType;
            this.displayName = displayName == null || displayName.isBlank() ? prettify(itemType) : displayName.trim();
            this.description = description == null ? "" : description.trim();
            this.maxStack = maxStack;
            this.usable = usable;
            this.placeable = placeable;
            this.buildType = buildType;
            this.healAmount = healAmount;
            this.attackDamage = attackDamage;
            this.toolPower = toolPower;
            this.captureModifier = captureModifier;
            this.categories = new LinkedHashSet<>(categories);
        }
    }
}
