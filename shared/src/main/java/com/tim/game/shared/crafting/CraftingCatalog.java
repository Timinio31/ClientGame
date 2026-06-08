package com.tim.game.shared.crafting;

import com.tim.game.shared.inventory.ItemCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * V1-Rezeptkatalog.
 *
 * Der Katalog ist bewusst breiter als die aktuelle UI: Er definiert den frühen Spiel-Loop
 * Sammeln -> Basic Crafting -> Workbench -> Workstations -> Energie/Fluids -> Bibble-Capture.
 * Später kann diese Klasse direkt durch DB-/JSON-Loading ersetzt werden.
 */
public final class CraftingCatalog {
    public static final String RECIPE_ROPE_FROM_FIBERS = "ROPE_FROM_FIBERS";
    public static final String RECIPE_PLANKS_FROM_WOOD = "PLANKS_FROM_WOOD";
    public static final String RECIPE_STICKS_FROM_WOOD = "STICKS_FROM_WOOD";
    public static final String RECIPE_PRIMITIVE_TOOL = "PRIMITIVE_TOOL";
    public static final String RECIPE_STONE_CLUB = "STONE_CLUB";
    public static final String RECIPE_STONE_SPEAR = "STONE_SPEAR";
    public static final String RECIPE_SIMPLE_MEAL = "SIMPLE_MEAL";
    public static final String RECIPE_WORKBENCH_KIT = "WORKBENCH_KIT";
    public static final String RECIPE_STORAGE_BOX_KIT = "STORAGE_BOX_KIT";
    public static final String RECIPE_WOOD_WALL_KIT = "WOOD_WALL_KIT";
    public static final String RECIPE_WOOD_FENCE_KIT = "WOOD_FENCE_KIT";
    public static final String RECIPE_FARMLAND_KIT = "FARMLAND_KIT";
    public static final String RECIPE_DIRT_BLOCK_KIT = "DIRT_BLOCK_KIT";

    public static final String RECIPE_PICKAXE = "PICKAXE";
    public static final String RECIPE_AXE = "AXE";
    public static final String RECIPE_SHOVEL = "SHOVEL";
    public static final String RECIPE_HAMMER = "HAMMER";
    public static final String RECIPE_WRENCH = "WRENCH";
    public static final String RECIPE_EMPTY_BUCKET = "EMPTY_BUCKET";
    public static final String RECIPE_BASIC_CAPTURE_NET = "BASIC_CAPTURE_NET";
    public static final String RECIPE_BASIC_TRAP = "BASIC_TRAP";
    public static final String RECIPE_BIBBLE_LEASH = "BIBBLE_LEASH";
    public static final String RECIPE_BIBBLE_FOOD = "BIBBLE_FOOD";
    public static final String RECIPE_FURNACE_KIT = "FURNACE_KIT";
    public static final String RECIPE_GENERATOR_KIT = "GENERATOR_KIT";
    public static final String RECIPE_BIBBLE_TERMINAL_KIT = "BIBBLE_TERMINAL_KIT";
    public static final String RECIPE_PUMP_KIT = "PUMP_KIT";
    public static final String RECIPE_CROP_FARMER_KIT = "CROP_FARMER_KIT";
    public static final String RECIPE_QUARRY_KIT = "QUARRY_KIT";
    public static final String RECIPE_WATER_WHEEL_KIT = "WATER_WHEEL_KIT";
    public static final String RECIPE_WIND_WHEEL_KIT = "WIND_WHEEL_KIT";

    public static final String RECIPE_IRON_INGOT = "IRON_INGOT_FROM_ORE";
    public static final String RECIPE_COPPER_INGOT = "COPPER_INGOT_FROM_ORE";
    public static final String RECIPE_COPPER_WIRE = "COPPER_WIRE";
    public static final String RECIPE_IRON_PLATE = "IRON_PLATE";
    public static final String RECIPE_BASIC_CABLE = "BASIC_CABLE";
    public static final String RECIPE_BASIC_PIPE = "BASIC_PIPE";
    public static final String RECIPE_PROCESSOR_KIT = "PROCESSOR_KIT";
    public static final String RECIPE_BURNER_FURNACE_KIT = "BURNER_FURNACE_KIT";
    public static final String RECIPE_HIGH_PERFORMANCE_FURNACE_KIT = "HIGH_PERFORMANCE_FURNACE_KIT";
    public static final String RECIPE_LAVA_PUMP_KIT = "LAVA_PUMP_KIT";
    public static final String RECIPE_OIL_PUMP_KIT = "OIL_PUMP_KIT";

    private static final List<CraftingRecipe> RECIPES = createRecipes();

    private CraftingCatalog() {
    }

    public static List<CraftingRecipe> allRecipes() {
        return new ArrayList<>(RECIPES);
    }

    public static List<CraftingRecipe> inventoryRecipes() {
        List<CraftingRecipe> result = new ArrayList<>();
        for (CraftingRecipe recipe : RECIPES) {
            if (recipe.isInventoryRecipe()) {
                result.add(recipe);
            }
        }
        return result;
    }

    public static CraftingRecipe findById(String recipeId) {
        if (recipeId == null || recipeId.isBlank()) {
            return null;
        }
        String normalized = recipeId.trim().toUpperCase();
        for (CraftingRecipe recipe : RECIPES) {
            if (normalized.equals(recipe.getRecipeId())) {
                return recipe;
            }
        }
        return null;
    }

    private static List<CraftingRecipe> createRecipes() {
        List<CraftingRecipe> recipes = new ArrayList<>();

        recipes.add(recipe(RECIPE_ROPE_FROM_FIBERS, "Simple Rope", "Turns flexible fiber material into rope.", CraftingRecipe.STATION_INVENTORY, 0, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_FIBER_MATERIAL, 3)),
                List.of(CraftingOutput.item(ItemCatalog.ROPE, 1))));

        recipes.add(recipe(RECIPE_PLANKS_FROM_WOOD, "Wooden Planks", "Splits basic wood into planks for crafting and construction.", CraftingRecipe.STATION_INVENTORY, 0, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_WOOD_MATERIAL, 2)),
                List.of(CraftingOutput.item(ItemCatalog.PLANK, 4))));

        recipes.add(recipe(RECIPE_STICKS_FROM_WOOD, "Sticks", "Creates simple handles from wood material.", CraftingRecipe.STATION_INVENTORY, 0, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_WOOD_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.STICK, 2))));

        recipes.add(recipe(RECIPE_PRIMITIVE_TOOL, "Primitive Tool", "A flexible early tool made from head material, a handle and binding.", CraftingRecipe.STATION_INVENTORY, 0, "",
                List.of(
                        CraftingIngredient.category(ItemCatalog.CATEGORY_TOOL_HEAD_MATERIAL, 1),
                        CraftingIngredient.category(ItemCatalog.CATEGORY_HANDLE_MATERIAL, 1),
                        CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)
                ),
                List.of(CraftingOutput.item(ItemCatalog.PRIMITIVE_TOOL, 1))));

        recipes.add(recipe(RECIPE_STONE_CLUB, "Stone Club", "Cheap early melee weapon.", CraftingRecipe.STATION_INVENTORY, 0, "",
                List.of(
                        CraftingIngredient.category(ItemCatalog.CATEGORY_STONE_MATERIAL, 2),
                        CraftingIngredient.category(ItemCatalog.CATEGORY_HANDLE_MATERIAL, 1),
                        CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)
                ),
                List.of(CraftingOutput.item(ItemCatalog.STONE_CLUB, 1))));

        recipes.add(recipe(RECIPE_STONE_SPEAR, "Stone Spear", "Simple weapon with better reach in later combat tuning.", CraftingRecipe.STATION_INVENTORY, 0, "",
                List.of(
                        CraftingIngredient.category(ItemCatalog.CATEGORY_TOOL_HEAD_MATERIAL, 1),
                        CraftingIngredient.item(ItemCatalog.STICK, 2),
                        CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)
                ),
                List.of(CraftingOutput.item(ItemCatalog.STONE_SPEAR, 1))));

        recipes.add(recipe(RECIPE_SIMPLE_MEAL, "Simple Meal", "Basic food from berries and fiber/plant material.", CraftingRecipe.STATION_INVENTORY, 0, "",
                List.of(
                        CraftingIngredient.category(ItemCatalog.CATEGORY_BERRY, 2),
                        CraftingIngredient.category(ItemCatalog.CATEGORY_FIBER_MATERIAL, 1)
                ),
                List.of(CraftingOutput.item(ItemCatalog.SIMPLE_MEAL, 1))));

        recipes.add(recipe(RECIPE_WORKBENCH_KIT, "Workbench Kit", "Prepared construction kit for placing a workbench.", CraftingRecipe.STATION_INVENTORY, 0, "WORKBENCH",
                List.of(
                        CraftingIngredient.item(ItemCatalog.PLANK, 6),
                        CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)
                ),
                List.of(CraftingOutput.item(ItemCatalog.WORKBENCH_KIT, 1))));

        recipes.add(recipe(RECIPE_STORAGE_BOX_KIT, "Storage Box Kit", "Basic placeable storage.", CraftingRecipe.STATION_INVENTORY, 0, "STORAGE_BOX",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 4), CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.STORAGE_BOX_KIT, 1))));

        recipes.add(recipe(RECIPE_WOOD_WALL_KIT, "Wood Wall", "Simple placeable wall segment.", CraftingRecipe.STATION_INVENTORY, 0, "WOOD_WALL",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 2)),
                List.of(CraftingOutput.item(ItemCatalog.WOOD_WALL_KIT, 2))));

        recipes.add(recipe(RECIPE_WOOD_FENCE_KIT, "Wood Fence", "Simple fence segment for Bibble areas.", CraftingRecipe.STATION_INVENTORY, 0, "WOOD_FENCE",
                List.of(CraftingIngredient.item(ItemCatalog.STICK, 3), CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.WOOD_FENCE_KIT, 2))));

        recipes.add(recipe(RECIPE_FARMLAND_KIT, "Farmland Tile", "Basic farm land marker.", CraftingRecipe.STATION_INVENTORY, 0, "FARMLAND",
                List.of(CraftingIngredient.item(ItemCatalog.DIRT, 3), CraftingIngredient.category(ItemCatalog.CATEGORY_FIBER_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.FARMLAND_KIT, 1))));

        recipes.add(recipe(RECIPE_DIRT_BLOCK_KIT, "Dirt Block", "Simple placeable dirt block.", CraftingRecipe.STATION_INVENTORY, 0, "DIRT_BLOCK",
                List.of(CraftingIngredient.item(ItemCatalog.DIRT, 1)),
                List.of(CraftingOutput.item(ItemCatalog.DIRT_BLOCK_KIT, 1))));

        recipes.add(recipe(RECIPE_PICKAXE, "Stone Pickaxe", "Workbench tool recipe using flexible component categories.", CraftingRecipe.STATION_WORKBENCH, 10, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_TOOL_HEAD_MATERIAL, 3), CraftingIngredient.category(ItemCatalog.CATEGORY_HANDLE_MATERIAL, 1), CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.PICKAXE, 1))));

        recipes.add(recipe(RECIPE_AXE, "Stone Axe", "Workbench axe recipe.", CraftingRecipe.STATION_WORKBENCH, 10, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_TOOL_HEAD_MATERIAL, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_HANDLE_MATERIAL, 1), CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.AXE, 1))));

        recipes.add(recipe(RECIPE_SHOVEL, "Stone Shovel", "Workbench shovel recipe.", CraftingRecipe.STATION_WORKBENCH, 10, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_TOOL_HEAD_MATERIAL, 1), CraftingIngredient.category(ItemCatalog.CATEGORY_HANDLE_MATERIAL, 1), CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.SHOVEL, 1))));

        recipes.add(recipe(RECIPE_HAMMER, "Hammer", "Building hammer for later build-mode extensions.", CraftingRecipe.STATION_WORKBENCH, 12, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_TOOL_HEAD_MATERIAL, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_HANDLE_MATERIAL, 1), CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.HAMMER, 1))));

        recipes.add(recipe(RECIPE_WRENCH, "Wrench", "Utility tool for machines and upgrades.", CraftingRecipe.STATION_WORKBENCH, 14, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_METAL_MATERIAL, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_HANDLE_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.WRENCH, 1))));

        recipes.add(recipe(RECIPE_EMPTY_BUCKET, "Empty Bucket", "Container for water, oil and lava.", CraftingRecipe.STATION_WORKBENCH, 10, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_WOOD_MATERIAL, 3), CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.EMPTY_BUCKET, 1))));

        recipes.add(recipe(RECIPE_BASIC_CAPTURE_NET, "Basic Capture Net", "Early Bibble capture item.", CraftingRecipe.STATION_WORKBENCH, 8, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_ROPE_MATERIAL, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_HANDLE_MATERIAL, 2)),
                List.of(CraftingOutput.item(ItemCatalog.BASIC_CAPTURE_NET, 1))));

        recipes.add(recipe(RECIPE_BASIC_TRAP, "Basic Trap", "Early capture trap placeholder.", CraftingRecipe.STATION_WORKBENCH, 12, "",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 3), CraftingIngredient.category(ItemCatalog.CATEGORY_ROPE_MATERIAL, 1), CraftingIngredient.category(ItemCatalog.CATEGORY_STONE_MATERIAL, 2)),
                List.of(CraftingOutput.item(ItemCatalog.BASIC_TRAP, 1))));

        recipes.add(recipe(RECIPE_BIBBLE_LEASH, "Bibble Leash", "Control and training item.", CraftingRecipe.STATION_WORKBENCH, 8, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_ROPE_MATERIAL, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_BERRY, 1)),
                List.of(CraftingOutput.item(ItemCatalog.BIBBLE_LEASH, 1))));

        recipes.add(recipe(RECIPE_BIBBLE_FOOD, "Bibble Food", "Prepared food for Bibble systems.", CraftingRecipe.STATION_WORKBENCH, 8, "",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_BERRY, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_FIBER_MATERIAL, 2)),
                List.of(CraftingOutput.item(ItemCatalog.BIBBLE_FOOD, 2))));

        recipes.add(recipe(RECIPE_FURNACE_KIT, "Furnace Kit", "Basic furnace for ore processing.", CraftingRecipe.STATION_WORKBENCH, 20, "FURNACE",
                List.of(CraftingIngredient.category(ItemCatalog.CATEGORY_STONE_MATERIAL, 8), CraftingIngredient.item(ItemCatalog.CLAY, 2)),
                List.of(CraftingOutput.item(ItemCatalog.FURNACE_KIT, 1))));

        recipes.add(recipe(RECIPE_GENERATOR_KIT, "Generator Kit", "Creates a placeable generator core.", CraftingRecipe.STATION_WORKBENCH, 20, "GENERATOR",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 4), CraftingIngredient.item(ItemCatalog.STONE, 4), CraftingIngredient.category(ItemCatalog.CATEGORY_POWER_COMPONENT, 1)),
                List.of(CraftingOutput.item(ItemCatalog.GENERATOR_KIT, 1))));

        recipes.add(recipe(RECIPE_BIBBLE_TERMINAL_KIT, "Bibble Terminal Kit", "Global Bibble storage access point.", CraftingRecipe.STATION_WORKBENCH, 20, "BIBBLE_TERMINAL",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 6), CraftingIngredient.item(ItemCatalog.ROPE, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_BERRY, 2)),
                List.of(CraftingOutput.item(ItemCatalog.BIBBLE_TERMINAL_KIT, 1))));

        recipes.add(recipe(RECIPE_PUMP_KIT, "Pump Kit", "Generic pump for water and compatible liquids.", CraftingRecipe.STATION_WORKBENCH, 18, "PUMP",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 4), CraftingIngredient.category(ItemCatalog.CATEGORY_FLUID_COMPONENT, 1), CraftingIngredient.category(ItemCatalog.CATEGORY_STONE_MATERIAL, 4)),
                List.of(CraftingOutput.item(ItemCatalog.PUMP_KIT, 1))));

        recipes.add(recipe(RECIPE_CROP_FARMER_KIT, "Crop Farmer Kit", "Early farming automation workstation.", CraftingRecipe.STATION_WORKBENCH, 18, "CROP_FARMER",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 5), CraftingIngredient.item(ItemCatalog.FARMLAND_KIT, 2), CraftingIngredient.item(ItemCatalog.WATER_BUCKET, 1)),
                List.of(CraftingOutput.item(ItemCatalog.CROP_FARMER_KIT, 1))));

        recipes.add(recipe(RECIPE_QUARRY_KIT, "Quarry Kit", "Early stone/ore automation workstation.", CraftingRecipe.STATION_WORKBENCH, 25, "QUARRY",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 6), CraftingIngredient.category(ItemCatalog.CATEGORY_TOOL_HEAD_MATERIAL, 6), CraftingIngredient.item(ItemCatalog.PICKAXE, 1)),
                List.of(CraftingOutput.item(ItemCatalog.QUARRY_KIT, 1))));

        recipes.add(recipe(RECIPE_WATER_WHEEL_KIT, "Water Wheel Kit", "Water-based power source.", CraftingRecipe.STATION_WORKBENCH, 20, "WATER_WHEEL",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 8), CraftingIngredient.category(ItemCatalog.CATEGORY_BINDING_MATERIAL, 2)),
                List.of(CraftingOutput.item(ItemCatalog.WATER_WHEEL_KIT, 1))));

        recipes.add(recipe(RECIPE_WIND_WHEEL_KIT, "Wind Wheel Kit", "Wind-based power source.", CraftingRecipe.STATION_WORKBENCH, 20, "WIND_WHEEL",
                List.of(CraftingIngredient.item(ItemCatalog.PLANK, 8), CraftingIngredient.item(ItemCatalog.ROPE, 2), CraftingIngredient.item(ItemCatalog.FIBER, 4)),
                List.of(CraftingOutput.item(ItemCatalog.WIND_WHEEL_KIT, 1))));

        recipes.add(recipe(RECIPE_IRON_INGOT, "Iron Ingot", "Smelts raw iron ore into ingots.", CraftingRecipe.STATION_MACHINE, 30, "",
                List.of(CraftingIngredient.item(ItemCatalog.RAW_IRON_ORE, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_FUEL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.IRON_INGOT, 1))));

        recipes.add(recipe(RECIPE_COPPER_INGOT, "Copper Ingot", "Smelts raw copper ore into ingots.", CraftingRecipe.STATION_MACHINE, 25, "",
                List.of(CraftingIngredient.item(ItemCatalog.RAW_COPPER_ORE, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_FUEL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.COPPER_INGOT, 1))));

        recipes.add(recipe(RECIPE_COPPER_WIRE, "Copper Wire", "Creates basic wire from copper.", CraftingRecipe.STATION_MACHINE, 16, "",
                List.of(CraftingIngredient.item(ItemCatalog.COPPER_INGOT, 1)),
                List.of(CraftingOutput.item(ItemCatalog.COPPER_WIRE, 3))));

        recipes.add(recipe(RECIPE_IRON_PLATE, "Iron Plate", "Creates machine casing plates.", CraftingRecipe.STATION_MACHINE, 16, "",
                List.of(CraftingIngredient.item(ItemCatalog.IRON_INGOT, 1)),
                List.of(CraftingOutput.item(ItemCatalog.IRON_PLATE, 2))));

        recipes.add(recipe(RECIPE_BASIC_CABLE, "Basic Cable", "Placeable cable for power networks.", CraftingRecipe.STATION_MACHINE, 12, "BASIC_CABLE",
                List.of(CraftingIngredient.item(ItemCatalog.COPPER_WIRE, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_FIBER_MATERIAL, 1)),
                List.of(CraftingOutput.item(ItemCatalog.BASIC_CABLE, 4))));

        recipes.add(recipe(RECIPE_BASIC_PIPE, "Basic Pipe", "Placeable pipe for fluid networks.", CraftingRecipe.STATION_MACHINE, 12, "BASIC_PIPE",
                List.of(CraftingIngredient.item(ItemCatalog.CLAY, 2), CraftingIngredient.item(ItemCatalog.IRON_PLATE, 1)),
                List.of(CraftingOutput.item(ItemCatalog.BASIC_PIPE, 4))));

        recipes.add(recipe(RECIPE_PROCESSOR_KIT, "Processor Kit", "Material processor workstation.", CraftingRecipe.STATION_MACHINE, 30, "PROCESSOR",
                List.of(CraftingIngredient.item(ItemCatalog.IRON_PLATE, 4), CraftingIngredient.item(ItemCatalog.COPPER_WIRE, 4), CraftingIngredient.item(ItemCatalog.PLANK, 4)),
                List.of(CraftingOutput.item(ItemCatalog.PROCESSOR_KIT, 1))));

        recipes.add(recipe(RECIPE_BURNER_FURNACE_KIT, "Burner Furnace Kit", "Fuel-based furnace upgrade and power source.", CraftingRecipe.STATION_MACHINE, 30, "BURNER_FURNACE",
                List.of(CraftingIngredient.item(ItemCatalog.FURNACE_KIT, 1), CraftingIngredient.item(ItemCatalog.IRON_PLATE, 2), CraftingIngredient.category(ItemCatalog.CATEGORY_FUEL, 4)),
                List.of(CraftingOutput.item(ItemCatalog.BURNER_FURNACE_KIT, 1))));

        recipes.add(recipe(RECIPE_HIGH_PERFORMANCE_FURNACE_KIT, "High Performance Furnace Kit", "Advanced furnace placeholder.", CraftingRecipe.STATION_MACHINE, 45, "HIGH_PERFORMANCE_FURNACE",
                List.of(CraftingIngredient.item(ItemCatalog.BURNER_FURNACE_KIT, 1), CraftingIngredient.item(ItemCatalog.IRON_PLATE, 6), CraftingIngredient.item(ItemCatalog.COPPER_WIRE, 6)),
                List.of(CraftingOutput.item(ItemCatalog.HIGH_PERFORMANCE_FURNACE_KIT, 1))));

        recipes.add(recipe(RECIPE_LAVA_PUMP_KIT, "Lava Pump Kit", "Specialized pump upgrade.", CraftingRecipe.STATION_MACHINE, 30, "LAVA_PUMP",
                List.of(CraftingIngredient.item(ItemCatalog.PUMP_KIT, 1), CraftingIngredient.item(ItemCatalog.IRON_PLATE, 3), CraftingIngredient.item(ItemCatalog.LAVA_BUCKET, 1)),
                List.of(CraftingOutput.item(ItemCatalog.LAVA_PUMP_KIT, 1))));

        recipes.add(recipe(RECIPE_OIL_PUMP_KIT, "Oil Pump Kit", "Specialized oil pump upgrade.", CraftingRecipe.STATION_MACHINE, 30, "OIL_PUMP",
                List.of(CraftingIngredient.item(ItemCatalog.PUMP_KIT, 1), CraftingIngredient.item(ItemCatalog.IRON_PLATE, 3), CraftingIngredient.item(ItemCatalog.OIL_BUCKET, 1)),
                List.of(CraftingOutput.item(ItemCatalog.OIL_PUMP_KIT, 1))));

        return recipes;
    }

    private static CraftingRecipe recipe(String recipeId,
                                         String displayName,
                                         String description,
                                         String stationType,
                                         int durationTicks,
                                         String resultingBuildType,
                                         List<CraftingIngredient> inputs,
                                         List<CraftingOutput> outputs) {
        return new CraftingRecipe(
                recipeId,
                displayName,
                description,
                stationType,
                "",
                durationTicks,
                false,
                false,
                true,
                resultingBuildType,
                inputs,
                outputs
        );
    }
}
