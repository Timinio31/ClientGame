package com.tim.game.server.world;

import com.tim.game.server.world.map.LayeredProceduralMapGenerator;
import com.tim.game.server.world.map.GeneratedMap;
import com.tim.game.server.world.map.MapGenerator;
import com.tim.game.server.world.map.MapTile;
import com.tim.game.server.world.map.storage.SavedMapRepository;
import com.tim.game.server.world.inventory.PlayerInventory;
import com.tim.game.server.world.bibble.BibbleManager;
import com.tim.game.shared.DTOs.update.BuildingStateDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.MapChunkDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.TileStateDto;
import com.tim.game.shared.DTOs.update.ItemStackDto;
import com.tim.game.shared.DTOs.update.WorldItemStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.DTOs.input.BibbleInputDto;
import com.tim.game.shared.DTOs.input.ActionInputDto;
import com.tim.game.shared.config.WorldSettings;
import com.tim.game.shared.crafting.CraftingCatalog;
import com.tim.game.shared.crafting.CraftingIngredient;
import com.tim.game.shared.crafting.CraftingOutput;
import com.tim.game.shared.crafting.CraftingRecipe;
import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;
import com.tim.game.shared.model.EntityId;
import com.tim.game.shared.model.EntityType;
import com.tim.game.shared.model.Vector2f;
import com.tim.game.shared.inventory.ItemCatalog;
import com.tim.game.shared.world.TileType;
import com.tim.game.shared.world.BiomeType;
import com.tim.game.shared.workstation.WorkstationCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorldState {

    private static final float DEFAULT_TILE_SIZE = 1.0f;

    private final String roomId;
    private long tick;

    private final Map<EntityId, EntityState> entities = new HashMap<>();
    private final Map<String, EntityId> playerEntities = new HashMap<>();
    private final Map<String, EntityId> occupiedTiles = new HashMap<>();
    private final Map<String, PlayerInventory> playerInventories = new HashMap<>();
    private final List<WorldItemStateDto> worldItems = new ArrayList<>();
    private final Map<String, Long> nextMovementAllowedTickByClient = new HashMap<>();

    private final GeneratedMap generatedMap;
    private final MapGenerator mapGenerator = new LayeredProceduralMapGenerator();
    private final SavedMapRepository savedMapRepository = new SavedMapRepository();
    private final WorldSettings worldSettings;
    private final BibbleManager bibbleManager;

    public WorldState(String roomId) {
        this(roomId, WorldSettings.defaults());
    }

    public WorldState(String roomId, WorldSettings worldSettings) {
        this.roomId = roomId;
        this.tick = 0L;
        this.worldSettings = worldSettings == null ? WorldSettings.defaults() : worldSettings.normalizedCopy();

        long seed = this.worldSettings.resolveSeed(roomId);
        this.bibbleManager = new BibbleManager(this.worldSettings, seed);
        this.generatedMap = loadConfiguredMap(roomId, seed);
        if (this.worldSettings.isSettlementsEnabled() && !this.worldSettings.usesFileMap()) {
            generateSettlements(seed);
        }
        if (this.worldSettings.isWorldItemsEnabled() && this.worldSettings.isStarterItemsEnabled()) {
            spawnStarterWorldItems(seed);
        }
        if (this.worldSettings.isBibblesEnabled() && this.worldSettings.isBibbleSpawningEnabled()) {
            bibbleManager.spawnWildBibblesAroundSpawns(this.generatedMap.getSpawnPoints());
        }
        if (!this.worldSettings.usesFileMap() && this.worldSettings.isSaveGeneratedMapOnStart()) {
            saveGeneratedMapSnapshot();
        }

        DebugConfig.log(DebugCategory.MAP,
                "Generated world for room " + roomId
                        + " settings=" + this.worldSettings.getWorldName()
                        + " seed=" + generatedMap.getSeed()
                        + " size=" + generatedMap.getWidth() + "x" + generatedMap.getHeight()
                        + " spawns=" + generatedMap.getSpawnPoints().size()
                        + " scenicBuildings=" + buildBuildingStateList().size());
    }

    private GeneratedMap loadConfiguredMap(String roomId, long seed) {
        Path configuredMapFile = resolveConfiguredMapFile();
        if (configuredMapFile != null && Files.exists(configuredMapFile)) {
            try {
                GeneratedMap loaded = savedMapRepository.load(roomId, configuredMapFile);
                DebugConfig.log(DebugCategory.MAP, "Loaded persisted map file " + configuredMapFile + " for room " + roomId);
                return loaded;
            } catch (Exception e) {
                System.err.println("[Server] Map file could not be loaded, falling back to procedural map: " + e.getMessage());
            }
        } else if (worldSettings.usesFileMap() && configuredMapFile != null) {
            System.err.println("[Server] Map file not found, falling back to procedural map: " + configuredMapFile);
        }

        int width = Math.max(8, worldSettings.getMapWidth());
        int height = Math.max(8, worldSettings.getMapHeight());
        float tileSize = worldSettings.getTileSize() > 0f ? worldSettings.getTileSize() : DEFAULT_TILE_SIZE;
        return mapGenerator.generate(roomId, width, height, tileSize, seed);
    }

    private void saveGeneratedMapSnapshot() {
        int tileCount = generatedMap.getWidth() * generatedMap.getHeight();
        if (worldSettings.isChunkStreamingEnabled() && tileCount > worldSettings.getMapInitFullTileLimit()) {
            DebugConfig.log(DebugCategory.MAP,
                    "Skipping full JSON map snapshot for chunk-streamed large map. tileCount=" + tileCount);
            return;
        }
        try {
            Path mapPath = resolveConfiguredMapFile();
            boolean configuredPath = mapPath != null;
            if (mapPath == null) {
                mapPath = savedMapRepository.defaultMapPath(worldSettings.getMapId());
            }
            if (configuredPath && Files.exists(mapPath)) {
                DebugConfig.log(DebugCategory.MAP, "Keeping existing persisted map snapshot at " + mapPath);
                return;
            }
            savedMapRepository.save(generatedMap, mapPath, worldSettings.getMapId());
            DebugConfig.log(DebugCategory.MAP, "Saved generated map snapshot to " + mapPath);
        } catch (Exception e) {
            System.err.println("[Server] Could not save generated map snapshot: " + e.getMessage());
        }
    }

    private Path resolveConfiguredMapFile() {
        if (worldSettings.getMapFile() == null || worldSettings.getMapFile().isBlank()) {
            return null;
        }
        try {
            return Path.of(worldSettings.getMapFile().trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    public WorldSettings getWorldSettings() {
        return worldSettings;
    }

    public void incrementTick() {
        tick++;
        bibbleManager.updateFollowing(buildPlayerEntityMap());
    }

    public long getTick() {
        return tick;
    }

    public String getRoomId() {
        return roomId;
    }

    public EntityId spawnPlayerForClient(String clientId, Vector2f startPosition) {
        EntityId id = EntityId.player(clientId);

        int startTileX = (int) Math.floor(startPosition.getX() / generatedMap.getTileSize());
        int startTileY = (int) Math.floor(startPosition.getY() / generatedMap.getTileSize());
        Vector2f snappedStartPos = tileCenter(startTileX, startTileY);

        EntityState state = new EntityState(
                id,
                EntityType.PLAYER,
                clientId,
                snappedStartPos,
                100f, 100f,
                100f, 100f
        );

        state.setTileX(startTileX);
        state.setTileY(startTileY);

        entities.put(id, state);
        playerEntities.put(clientId, id);
        playerInventories.computeIfAbsent(clientId, ignored -> PlayerInventory.starterInventory());
        bibbleManager.ensurePlayerInitialized(clientId, state);

        DebugConfig.log(DebugCategory.MOVEMENT,
                "spawned player client=" + clientId
                        + " tile=(" + startTileX + "," + startTileY + ")"
                        + " worldPos=" + snappedStartPos);

        return id;
    }

    public EntityState getPlayerEntity(String clientId) {
        EntityId id = playerEntities.get(clientId);
        if (id == null) {
            return null;
        }
        return entities.get(id);
    }

    public void movePlayerStep(String clientId, int directionX, int directionY) {
        if (!worldSettings.isMovementEnabled()) {
            DebugConfig.log(DebugCategory.MOVEMENT, "movement disabled by world settings client=" + clientId);
            return;
        }

        long nextAllowedTick = nextMovementAllowedTickByClient.getOrDefault(clientId, 0L);
        if (tick < nextAllowedTick) {
            return;
        }
        nextMovementAllowedTickByClient.put(clientId, tick + worldSettings.getMovementCooldownTicks());

        if (Math.abs(directionX) + Math.abs(directionY) != 1) {
            return;
        }

        EntityState player = getPlayerEntity(clientId);
        if (player == null) {
            spawnPlayerForClient(clientId, getDefaultSpawnPoint());
            player = getPlayerEntity(clientId);
            if (player == null) {
                DebugConfig.log(DebugCategory.MOVEMENT,
                        "failed to spawn player for client=" + clientId);
                return;
            }
        }

        int playerX = player.getTileX() != null ? player.getTileX() : 0;
        int playerY = player.getTileY() != null ? player.getTileY() : 0;

        int targetX = playerX + directionX;
        int targetY = playerY + directionY;

        MapTile targetTile = generatedMap.getTile(targetX, targetY);
        if (targetTile == null) {
            DebugConfig.log(DebugCategory.MOVEMENT,
                    "blocked: target outside map client=" + clientId + " target=(" + targetX + "," + targetY + ")");
            return;
        }

        if (isTileBlocked(playerX, playerY, targetX, targetY)) {
            MapTile currentTile = generatedMap.getTile(playerX, playerY);
            DebugConfig.log(DebugCategory.MOVEMENT,
                    "blocked client=" + clientId
                            + " from=(" + playerX + "," + playerY + ")"
                            + " to=(" + targetX + "," + targetY + ")"
                            + " type=" + targetTile.getType()
                            + " walkable=" + targetTile.isWalkable()
                            + " heightFrom=" + (currentTile == null ? "?" : currentTile.getHeightLevel())
                            + " heightTo=" + targetTile.getHeightLevel()
                            + " occupied=" + occupiedTiles.containsKey(tileKey(targetX, targetY)));
            return;
        }

        player.setTileX(targetX);
        player.setTileY(targetY);
        player.setPosition(tileCenter(targetX, targetY));

        DebugConfig.log(DebugCategory.MOVEMENT,
                "client=" + clientId
                        + " from=(" + playerX + "," + playerY + ")"
                        + " to=(" + targetX + "," + targetY + ")");
    }

    public boolean placeBuilding(String clientId, String buildingType, int tileX, int tileY) {
        if (!worldSettings.isBuildingEnabled()) {
            DebugConfig.log(DebugCategory.BUILDING, "BUILD rejected: building disabled client=" + clientId);
            return false;
        }

        buildingType = WorkstationCatalog.normalizeType(buildingType);
        if (buildingType.isBlank()) {
            DebugConfig.log(DebugCategory.BUILDING, "BUILD rejected: missing building type client=" + clientId);
            return false;
        }

        String requiredItemType = ItemCatalog.getRequiredItemForBuilding(buildingType);
        PlayerInventory inventory = getOrCreateInventory(clientId);
        if (requiredItemType != null && inventory.countItem(requiredItemType) <= 0) {
            DebugConfig.log(DebugCategory.BUILDING,
                    "BUILD rejected: missing item client=" + clientId
                            + " required=" + requiredItemType
                            + " tile=(" + tileX + "," + tileY + ")");
            return false;
        }

        if (!generatedMap.isWalkable(tileX, tileY)) {
            DebugConfig.log(DebugCategory.BUILDING,
                    "BUILD rejected: tile not walkable client=" + clientId + " tile=(" + tileX + "," + tileY + ")");
            return false;
        }

        String key = tileKey(tileX, tileY);
        if (occupiedTiles.containsKey(key)) {
            DebugConfig.log(DebugCategory.BUILDING,
                    "BUILD rejected: occupied client=" + clientId + " tile=(" + tileX + "," + tileY + ")");
            return false;
        }

        EntityId id = EntityId.random();
        Vector2f position = tileCenter(tileX, tileY);

        int maxHealth = WorkstationCatalog.defaultMaxHealth(buildingType);
        EntityState state = new EntityState(
                id,
                EntityType.BUILDING,
                clientId,
                position,
                maxHealth, maxHealth,
                0f, 0f
        );

        state.setBuildingType(buildingType);
        state.setTileX(tileX);
        state.setTileY(tileY);

        entities.put(id, state);
        occupiedTiles.put(key, id);
        if (requiredItemType != null) {
            inventory.consumeItem(requiredItemType, 1);
        }

        DebugConfig.log(DebugCategory.BUILDING,
                "BUILD placed client=" + clientId
                        + " type=" + buildingType
                        + " tile=(" + tileX + "," + tileY + ")");

        return true;
    }



    public boolean handleBibbleInput(String clientId, BibbleInputDto input) {
        EntityState player = getOrSpawnPlayer(clientId);
        return bibbleManager.handleInput(clientId, input, player, buildBuildingEntityList(), getOrCreateInventory(clientId));
    }

    public boolean handlePlayerAction(String clientId, ActionInputDto input) {
        if (!worldSettings.isPlayerInteractionEnabled() || input == null) {
            return false;
        }
        String action = input.getAction() == null ? "" : input.getAction().trim().toUpperCase();
        return switch (action) {
            case "INTERACT", "PICKUP" -> pickupNearestWorldItem(clientId);
            case "HARVEST" -> harvestTile(clientId, worldToTile(input.getTargetX()), worldToTile(input.getTargetY()));
            case "ATTACK", "BREAK" -> attackOrHarvestTarget(clientId, worldToTile(input.getTargetX()), worldToTile(input.getTargetY()));
            case "USE_ITEM" -> useSelectedInventoryItem(clientId);
            default -> false;
        };
    }


    public boolean craftRecipe(String clientId, String recipeId, String context, String targetEntityId) {
        if (!worldSettings.isInventoryEnabled() || !worldSettings.isCraftingEnabled() || !worldSettings.isCraftingMenuEnabled()) {
            return false;
        }

        CraftingRecipe recipe = CraftingCatalog.findById(recipeId);
        if (recipe == null || !recipe.isUnlockedByDefault()) {
            return false;
        }
        if (!isRecipeTypeAllowedBySettings(recipe)) {
            return false;
        }
        if (!isCraftingContextValid(clientId, recipe, context, targetEntityId)) {
            return false;
        }

        EntityState player = getOrSpawnPlayer(clientId);
        if (player == null) {
            return false;
        }

        PlayerInventory inventory = getOrCreateInventory(clientId);
        for (CraftingIngredient ingredient : recipe.getInputs()) {
            if (!inventory.hasIngredient(ingredient)) {
                DebugConfig.log(DebugCategory.INVENTORY,
                        "CRAFT rejected: missing ingredient client=" + clientId + " recipe=" + recipe.getRecipeId());
                return false;
            }
        }

        if (!worldSettings.isWorldItemsEnabled() && !canPlaceAllOutputsInInventory(inventory, recipe)) {
            DebugConfig.log(DebugCategory.INVENTORY,
                    "CRAFT rejected: no room and world item drops disabled client=" + clientId + " recipe=" + recipe.getRecipeId());
            return false;
        }

        for (CraftingIngredient ingredient : recipe.getInputs()) {
            if (!inventory.consumeIngredient(ingredient)) {
                return false;
            }
        }

        int playerX = player.getTileX() == null ? 0 : player.getTileX();
        int playerY = player.getTileY() == null ? 0 : player.getTileY();
        for (CraftingOutput output : recipe.getOutputs()) {
            ItemStackDto crafted = ItemCatalog.createStack(output.getItemType(), output.getQuantity());
            int remaining = inventory.addStack(crafted);
            if (remaining > 0 && worldSettings.isWorldItemsEnabled()) {
                dropWorldItem(ItemCatalog.createStack(output.getItemType(), remaining), playerX, playerY);
                DebugConfig.log(DebugCategory.INVENTORY,
                        "CRAFT output overflow dropped client=" + clientId
                                + " recipe=" + recipe.getRecipeId()
                                + " item=" + output.getItemType()
                                + " amount=" + remaining);
            }
        }

        DebugConfig.log(DebugCategory.INVENTORY,
                "CRAFT completed client=" + clientId + " recipe=" + recipe.getRecipeId() + " context=" + context);
        return true;
    }

    private boolean isRecipeTypeAllowedBySettings(CraftingRecipe recipe) {
        if (recipe.isInventoryRecipe() && !worldSettings.isInventoryCraftingEnabled()) {
            return false;
        }
        if (recipe.isWorkstationRecipe() && !worldSettings.isWorkstationCraftingEnabled()) {
            return false;
        }
        if (recipe.isToolRecipe() && !worldSettings.isToolCraftingEnabled()) {
            return false;
        }
        if (recipe.isQualityEnabled() && !worldSettings.isCraftingQualityEnabled()) {
            return false;
        }
        return !recipe.isFailureEnabled() || worldSettings.isCraftingFailureEnabled();
    }

    private boolean isCraftingContextValid(String clientId, CraftingRecipe recipe, String context, String targetEntityId) {
        String recipeStation = CraftingRecipe.normalizeStation(recipe.getStationType());
        String requestedContext = CraftingRecipe.normalizeStation(context);

        if (recipe.isInventoryRecipe()) {
            return CraftingRecipe.STATION_INVENTORY.equals(requestedContext);
        }

        EntityState player = getOrSpawnPlayer(clientId);
        if (player == null) {
            return false;
        }

        if (targetEntityId != null && !targetEntityId.isBlank()) {
            EntityState target = findEntityByStringId(targetEntityId);
            return isUsableCraftingStationNearPlayer(player, target, recipeStation);
        }

        return isNearbyCraftingStationAvailable(player, recipeStation);
    }

    private EntityState findEntityByStringId(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        for (EntityState state : entities.values()) {
            if (state != null && state.getId() != null && entityId.equals(state.getId().getValue())) {
                return state;
            }
        }
        return null;
    }

    private boolean isNearbyCraftingStationAvailable(EntityState player, String stationType) {
        for (EntityState state : entities.values()) {
            if (isUsableCraftingStationNearPlayer(player, state, stationType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsableCraftingStationNearPlayer(EntityState player, EntityState station, String stationType) {
        if (player == null || station == null || station.getType() != EntityType.BUILDING) {
            return false;
        }

        String buildingType = station.getBuildingType();
        boolean stationMatches = WorkstationCatalog.matchesCraftingStation(buildingType, stationType);
        if (!stationMatches) {
            return false;
        }

        int playerX = player.getTileX() == null ? 0 : player.getTileX();
        int playerY = player.getTileY() == null ? 0 : player.getTileY();
        int stationX = station.getTileX() == null ? 0 : station.getTileX();
        int stationY = station.getTileY() == null ? 0 : station.getTileY();
        int distance = Math.abs(playerX - stationX) + Math.abs(playerY - stationY);
        return distance <= 1;
    }

    private boolean canPlaceAllOutputsInInventory(PlayerInventory inventory, CraftingRecipe recipe) {
        Map<String, Integer> requiredSpaceByItem = new HashMap<>();
        for (CraftingOutput output : recipe.getOutputs()) {
            String itemType = ItemCatalog.normalizeType(output.getItemType());
            requiredSpaceByItem.merge(itemType, output.getQuantity(), Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : requiredSpaceByItem.entrySet()) {
            if (inventory.availableSpaceForItem(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public void selectInventorySlot(String clientId, int slotIndex) {
        if (!worldSettings.isInventoryEnabled()) {
            return;
        }
        getOrCreateInventory(clientId).selectSlot(slotIndex);
    }

    public boolean useSelectedInventoryItem(String clientId) {
        if (!worldSettings.isInventoryEnabled()) {
            return false;
        }

        EntityState player = getOrSpawnPlayer(clientId);
        if (player == null) {
            return false;
        }

        PlayerInventory inventory = getOrCreateInventory(clientId);
        ItemStackDto selectedItem = inventory.getSelectedItem();
        if (selectedItem == null || !selectedItem.isUsable()) {
            return false;
        }

        String itemType = ItemCatalog.normalizeType(selectedItem.getItemType());
        int healAmount = ItemCatalog.getHealAmount(itemType);
        float healedHealth = healAmount <= 0 ? player.getHealth() : Math.min(player.getMaxHealth(), player.getHealth() + healAmount);
        boolean consumed = false;
        if (healAmount > 0 && healedHealth > player.getHealth()) {
            player.setHealth(healedHealth);
            consumed = true;
        }

        if (ItemCatalog.WATER_BUCKET.equals(itemType)) {
            // V1 placeholder: water bucket is usable, but does not yet place fluids.
            consumed = true;
        }

        if (!consumed) {
            return false;
        }

        inventory.removeFromSlot(inventory.getSelectedSlot(), 1);
        return true;
    }

    public boolean dropSelectedInventoryItem(String clientId, int amount) {
        if (!worldSettings.isInventoryEnabled() || !worldSettings.isWorldItemsEnabled()) {
            return false;
        }

        EntityState player = getOrSpawnPlayer(clientId);
        if (player == null) {
            return false;
        }

        PlayerInventory inventory = getOrCreateInventory(clientId);
        ItemStackDto removed = inventory.removeFromSlot(inventory.getSelectedSlot(), Math.max(1, amount));
        if (removed == null || removed.isEmpty()) {
            return false;
        }

        int tileX = player.getTileX() == null ? 0 : player.getTileX();
        int tileY = player.getTileY() == null ? 0 : player.getTileY();
        dropWorldItem(removed, tileX, tileY);
        return true;
    }

    public boolean pickupNearestWorldItem(String clientId) {
        if (!worldSettings.isInventoryEnabled() || !worldSettings.isWorldItemsEnabled()) {
            return false;
        }

        EntityState player = getOrSpawnPlayer(clientId);
        if (player == null) {
            return false;
        }

        int playerX = player.getTileX() == null ? 0 : player.getTileX();
        int playerY = player.getTileY() == null ? 0 : player.getTileY();
        WorldItemStateDto nearestItem = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (WorldItemStateDto worldItem : worldItems) {
            if (worldItem == null || worldItem.getItem() == null || worldItem.getItem().isEmpty()) {
                continue;
            }

            int distance = Math.abs(worldItem.getTileX() - playerX) + Math.abs(worldItem.getTileY() - playerY);
            if (distance <= 1 && distance < nearestDistance) {
                nearestItem = worldItem;
                nearestDistance = distance;
            }
        }

        if (nearestItem == null) {
            return false;
        }

        PlayerInventory inventory = getOrCreateInventory(clientId);
        int remaining = inventory.addStack(nearestItem.getItem());
        if (remaining <= 0) {
            worldItems.remove(nearestItem);
        } else {
            nearestItem.setItem(ItemCatalog.createStack(nearestItem.getItem().getItemType(), remaining));
        }
        return true;
    }

    private boolean attackOrHarvestTarget(String clientId, int targetTileX, int targetTileY) {
        if (!worldSettings.isCombatEnabled() && !worldSettings.isPlayerInteractionEnabled()) {
            return false;
        }
        EntityState player = getOrSpawnPlayer(clientId);
        if (player == null || !isTargetInActionRange(player, targetTileX, targetTileY, 2)) {
            return false;
        }

        PlayerInventory inventory = getOrCreateInventory(clientId);
        ItemStackDto selected = inventory.getSelectedItem();
        String itemType = selected == null ? "" : ItemCatalog.normalizeType(selected.getItemType());
        int damage = Math.max(3, ItemCatalog.getAttackDamage(itemType));

        EntityState building = findBuildingAt(targetTileX, targetTileY);
        if (building != null) {
            building.setHealth(Math.max(0f, building.getHealth() - damage));
            DebugConfig.log(DebugCategory.BUILDING,
                    "ACTION hit building client=" + clientId
                            + " target=" + building.getBuildingType()
                            + " damage=" + damage
                            + " remaining=" + building.getHealth());
            if (building.getHealth() <= 0f) {
                removeBuildingAndRefund(clientId, building);
            }
            return true;
        }

        if (bibbleManager.attackBibbleAt(clientId, player, targetTileX, targetTileY, damage)) {
            return true;
        }

        return harvestTile(clientId, targetTileX, targetTileY);
    }

    private boolean harvestTile(String clientId, int targetTileX, int targetTileY) {
        EntityState player = getOrSpawnPlayer(clientId);
        if (player == null || !isTargetInActionRange(player, targetTileX, targetTileY, 2)) {
            return false;
        }
        MapTile tile = generatedMap.getTile(targetTileX, targetTileY);
        if (tile == null) {
            return false;
        }

        PlayerInventory inventory = getOrCreateInventory(clientId);
        ItemStackDto selected = inventory.getSelectedItem();
        String selectedType = selected == null ? "" : ItemCatalog.normalizeType(selected.getItemType());
        String harvestedItem = null;
        int quantity = 1;

        if (tile.hasResource()) {
            harvestedItem = tile.getResourceType();
            quantity = Math.max(1, Math.min(tile.getResourceAmount(), harvestQuantityForResource(harvestedItem, selectedType)));
            tile.consumeResource(quantity);
            return addOrDrop(clientId, harvestedItem, quantity, targetTileX, targetTileY);
        }

        if (tile.getType() == TileType.FOREST || tile.getType() == TileType.GRASS || tile.getType() == TileType.SPAWN || tile.getType() == TileType.SWAMP || tile.getType() == TileType.SNOW) {
            if (ItemCatalog.AXE.equals(selectedType)) {
                harvestedItem = ItemCatalog.WOOD;
                quantity = 3;
            } else if (ItemCatalog.PRIMITIVE_TOOL.equals(selectedType)) {
                harvestedItem = ItemCatalog.WOOD;
                quantity = 2;
            } else {
                harvestedItem = targetTileX % 5 == 0 ? ItemCatalog.BERRY : ItemCatalog.FIBER;
                quantity = 1;
            }
        } else if (tile.getType() == TileType.MOUNTAIN || tile.getType() == TileType.WALL || tile.getType() == TileType.CLIFF || tile.getType() == TileType.SAND || tile.getType() == TileType.BEACH) {
            if (ItemCatalog.PICKAXE.equals(selectedType) || ItemCatalog.PRIMITIVE_TOOL.equals(selectedType)) {
                harvestedItem = oreOrStoneFor(targetTileX, targetTileY);
                quantity = ItemCatalog.PICKAXE.equals(selectedType) ? 2 : 1;
            } else {
                harvestedItem = ItemCatalog.STONE;
                quantity = 1;
            }
        } else if (tile.getType() == TileType.WATER) {
            if (!ItemCatalog.EMPTY_BUCKET.equals(selectedType)) {
                return false;
            }
            inventory.removeFromSlot(inventory.getSelectedSlot(), 1);
            return addOrDrop(clientId, ItemCatalog.WATER_BUCKET, 1, targetTileX, targetTileY);
        }

        if (harvestedItem == null) {
            return false;
        }
        return addOrDrop(clientId, harvestedItem, quantity, targetTileX, targetTileY);
    }

    private int harvestQuantityForResource(String harvestedItem, String selectedType) {
        String itemType = ItemCatalog.normalizeType(harvestedItem);
        if (ItemCatalog.WOOD.equals(itemType)) {
            return ItemCatalog.AXE.equals(selectedType) ? 3 : ItemCatalog.PRIMITIVE_TOOL.equals(selectedType) ? 2 : 1;
        }
        if (ItemCatalog.STONE.equals(itemType)
                || ItemCatalog.RAW_IRON_ORE.equals(itemType)
                || ItemCatalog.RAW_COPPER_ORE.equals(itemType)
                || ItemCatalog.COAL.equals(itemType)) {
            return ItemCatalog.PICKAXE.equals(selectedType) ? 2 : 1;
        }
        return 1;
    }

    private String oreOrStoneFor(int tileX, int tileY) {
        int roll = Math.floorMod(tileX * 31 + tileY * 17 + (int) tick, 100);
        if (roll < 5) {
            return ItemCatalog.RAW_IRON_ORE;
        }
        if (roll < 10) {
            return ItemCatalog.RAW_COPPER_ORE;
        }
        if (roll < 16) {
            return ItemCatalog.COAL;
        }
        return ItemCatalog.STONE;
    }

    private boolean addOrDrop(String clientId, String itemType, int quantity, int tileX, int tileY) {
        PlayerInventory inventory = getOrCreateInventory(clientId);
        int remaining = inventory.addStack(ItemCatalog.createStack(itemType, quantity));
        if (remaining > 0 && worldSettings.isWorldItemsEnabled()) {
            dropWorldItem(ItemCatalog.createStack(itemType, remaining), tileX, tileY);
        }
        return remaining < quantity || worldSettings.isWorldItemsEnabled();
    }

    private boolean isTargetInActionRange(EntityState player, int targetTileX, int targetTileY, int maxDistance) {
        int playerX = player.getTileX() == null ? 0 : player.getTileX();
        int playerY = player.getTileY() == null ? 0 : player.getTileY();
        int distance = Math.abs(playerX - targetTileX) + Math.abs(playerY - targetTileY);
        return distance <= maxDistance;
    }

    private EntityState findBuildingAt(int tileX, int tileY) {
        EntityId id = occupiedTiles.get(tileKey(tileX, tileY));
        if (id == null) {
            return null;
        }
        EntityState state = entities.get(id);
        return state != null && state.getType() == EntityType.BUILDING ? state : null;
    }

    private void removeBuildingAndRefund(String clientId, EntityState building) {
        if (building == null || building.getId() == null) {
            return;
        }
        int tileX = building.getTileX() == null ? 0 : building.getTileX();
        int tileY = building.getTileY() == null ? 0 : building.getTileY();
        entities.remove(building.getId());
        occupiedTiles.remove(tileKey(tileX, tileY));

        String refundItem = ItemCatalog.getRequiredItemForBuilding(building.getBuildingType());
        if (refundItem != null) {
            addOrDrop(clientId, refundItem, 1, tileX, tileY);
        }
    }

    private int worldToTile(float worldValue) {
        float tileSize = generatedMap == null || generatedMap.getTileSize() <= 0f ? DEFAULT_TILE_SIZE : generatedMap.getTileSize();
        return (int) Math.floor(worldValue / tileSize);
    }

    private EntityState getOrSpawnPlayer(String clientId) {
        EntityState player = getPlayerEntity(clientId);
        if (player != null) {
            return player;
        }

        spawnPlayerForClient(clientId, getDefaultSpawnPoint());
        return getPlayerEntity(clientId);
    }

    private PlayerInventory getOrCreateInventory(String clientId) {
        return playerInventories.computeIfAbsent(clientId, ignored -> PlayerInventory.starterInventory());
    }

    private void dropWorldItem(ItemStackDto item, int tileX, int tileY) {
        if (item == null || item.isEmpty()) {
            return;
        }

        WorldItemStateDto worldItem = new WorldItemStateDto(
                EntityId.random().getValue(),
                ItemCatalog.createStack(item.getItemType(), item.getQuantity()),
                tileX,
                tileY,
                tileCenter(tileX, tileY)
        );
        worldItems.add(worldItem);
    }

    private List<WorldItemStateDto> buildWorldItemStateList() {
        List<WorldItemStateDto> result = new ArrayList<>();
        for (WorldItemStateDto worldItem : worldItems) {
            if (worldItem == null || worldItem.getItem() == null || worldItem.getItem().isEmpty()) {
                continue;
            }
            result.add(new WorldItemStateDto(
                    worldItem.getEntityId(),
                    worldItem.getItem().copy(),
                    worldItem.getTileX(),
                    worldItem.getTileY(),
                    worldItem.getPosition()
            ));
        }
        return result;
    }

    public WorldSnapshotDto buildSnapshot() {
        return buildDynamicSnapshot(tick);
    }

    public WorldSnapshotDto buildDynamicSnapshot(long tick) {
        WorldSnapshotDto dto = new WorldSnapshotDto();
        dto.setRoomId(roomId);
        dto.setTick(tick);
        dto.setPlayers(buildPlayerStateList());
        dto.setBuildings(buildBuildingStateList());
        dto.setWorldItems(buildWorldItemStateList());
        dto.setBibbles(bibbleManager.buildSnapshotBibbles());
        return dto;
    }

    public MapInitDto buildMapInit() {
        MapInitDto dto = new MapInitDto(
                generatedMap.getRoomId(),
                generatedMap.getWidth(),
                generatedMap.getHeight(),
                generatedMap.getTileSize(),
                generatedMap.getSeed(),
                worldSettings
        );

        int tileCount = generatedMap.getWidth() * generatedMap.getHeight();
        boolean sendFullMap = !worldSettings.isChunkStreamingEnabled()
                || tileCount <= worldSettings.getMapInitFullTileLimit();

        if (sendFullMap) {
            for (int x = 0; x < generatedMap.getWidth(); x++) {
                for (int y = 0; y < generatedMap.getHeight(); y++) {
                    MapTile tile = generatedMap.getTile(x, y);
                    if (tile == null) {
                        continue;
                    }
                    dto.addTile(toTileStateDto(tile));
                }
            }
        }

        for (Vector2f spawnPoint : generatedMap.getSpawnPoints()) {
            dto.addSpawnPoint(spawnPoint);
        }

        return dto;
    }

    public List<MapChunkDto> buildMapChunks(int minChunkX, int maxChunkX, int minChunkY, int maxChunkY) {
        List<MapChunkDto> chunks = new ArrayList<>();
        int chunkSize = Math.max(1, worldSettings.getChunkSize());
        int lastChunkX = Math.max(0, (generatedMap.getWidth() - 1) / chunkSize);
        int lastChunkY = Math.max(0, (generatedMap.getHeight() - 1) / chunkSize);

        int safeMinX = Math.max(0, Math.min(minChunkX, maxChunkX));
        int safeMaxX = Math.min(lastChunkX, Math.max(minChunkX, maxChunkX));
        int safeMinY = Math.max(0, Math.min(minChunkY, maxChunkY));
        int safeMaxY = Math.min(lastChunkY, Math.max(minChunkY, maxChunkY));

        int maxChunksPerRequest = 49;
        int sent = 0;
        for (int chunkX = safeMinX; chunkX <= safeMaxX; chunkX++) {
            for (int chunkY = safeMinY; chunkY <= safeMaxY; chunkY++) {
                if (sent >= maxChunksPerRequest) {
                    return chunks;
                }
                chunks.add(buildMapChunk(chunkX, chunkY));
                sent++;
            }
        }
        return chunks;
    }

    private MapChunkDto buildMapChunk(int chunkX, int chunkY) {
        int chunkSize = Math.max(1, worldSettings.getChunkSize());
        MapChunkDto dto = new MapChunkDto(
                generatedMap.getRoomId(),
                chunkX,
                chunkY,
                chunkSize,
                generatedMap.getWidth(),
                generatedMap.getHeight()
        );

        int startX = chunkX * chunkSize;
        int startY = chunkY * chunkSize;
        int endX = Math.min(generatedMap.getWidth(), startX + chunkSize);
        int endY = Math.min(generatedMap.getHeight(), startY + chunkSize);

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                MapTile tile = generatedMap.getTile(x, y);
                if (tile != null) {
                    dto.addTile(toTileStateDto(tile));
                }
            }
        }
        return dto;
    }

    private TileStateDto toTileStateDto(MapTile tile) {
        return new TileStateDto(
                tile.getX(),
                tile.getY(),
                tile.getType().name(),
                tile.isWalkable(),
                tile.getHeightLevel(),
                tile.getBiome() == null ? null : tile.getBiome().name(),
                tile.getResourceType(),
                tile.getResourceAmount(),
                tile.getFeature()
        );
    }

    private Map<String, EntityState> buildPlayerEntityMap() {
        Map<String, EntityState> result = new HashMap<>();
        for (Map.Entry<String, EntityId> entry : playerEntities.entrySet()) {
            EntityState state = entities.get(entry.getValue());
            if (state != null) {
                result.put(entry.getKey(), state);
            }
        }
        return result;
    }

    private List<EntityState> buildBuildingEntityList() {
        List<EntityState> result = new ArrayList<>();
        for (EntityState state : entities.values()) {
            if (state != null && state.getType() == EntityType.BUILDING) {
                result.add(state);
            }
        }
        return result;
    }

    private List<PlayerStateDto> buildPlayerStateList() {
        List<PlayerStateDto> result = new ArrayList<>();

        for (Map.Entry<String, EntityId> entry : playerEntities.entrySet()) {
            String clientId = entry.getKey();
            EntityId entityId = entry.getValue();
            EntityState state = entities.get(entityId);

            if (state == null) {
                continue;
            }

            PlayerStateDto dto = new PlayerStateDto(
                    state.getId().getValue(),
                    clientId,
                    state.getType(),
                    state.getPosition(),
                    state.getHealth(),
                    state.getMaxHealth(),
                    state.getStamina(),
                    state.getMaxStamina()
            );
            if (worldSettings.isInventoryEnabled()) {
                PlayerInventory inventory = getOrCreateInventory(clientId);
                dto.setInventorySlots(inventory.toDtoSlots());
                dto.setSelectedInventorySlot(inventory.getSelectedSlot());
            }
            dto.setBibbleInventory(bibbleManager.buildInventoryDto(clientId));
            result.add(dto);
        }

        return result;
    }

    private List<BuildingStateDto> buildBuildingStateList() {
        List<BuildingStateDto> result = new ArrayList<>();

        for (EntityState state : entities.values()) {
            if (state.getType() != EntityType.BUILDING) {
                continue;
            }

            result.add(new BuildingStateDto(
                    state.getId().getValue(),
                    state.getClientId(),
                    state.getBuildingType(),
                    state.getTileX() == null ? 0 : state.getTileX(),
                    state.getTileY() == null ? 0 : state.getTileY(),
                    state.getPosition()
            ));
        }

        return result;
    }

    private void generateSettlements(long seed) {
        Random random = new Random(seed ^ 0x51A77E3DL);
        generatedMap.getSpawnPoints().clear();

        List<int[]> centers = pickSettlementCenters(random);
        for (int i = 0; i < centers.size(); i++) {
            int[] center = centers.get(i);
            createSettlement(center[0], center[1], random, i == 0, i);
        }

        for (int i = 1; i < centers.size(); i++) {
            int[] previous = centers.get(i - 1);
            int[] current = centers.get(i);
            connectSettlements(previous[0], previous[1], current[0], current[1]);
        }
        if (centers.size() > 2) {
            int[] hub = centers.get(0);
            for (int i = 2; i < centers.size(); i += 2) {
                int[] current = centers.get(i);
                connectSettlements(hub[0], hub[1], current[0], current[1]);
            }
        }
    }

    private List<int[]> pickSettlementCenters(Random random) {
        List<int[]> centers = new ArrayList<>();
        int width = generatedMap.getWidth();
        int height = generatedMap.getHeight();
        int baseCount = Math.max(3, Math.min(12, (width * height) / 22000));
        int minDistance = Math.max(18, Math.min(width, height) / Math.max(4, baseCount));

        int centerX = width / 2;
        int centerY = height / 2;
        centers.add(new int[]{centerX, centerY});

        int attempts = baseCount * 80;
        while (centers.size() < baseCount && attempts-- > 0) {
            int x = 8 + random.nextInt(Math.max(1, width - 16));
            int y = 8 + random.nextInt(Math.max(1, height - 16));
            if (!isValidSettlementCenter(x, y)) {
                continue;
            }
            boolean tooClose = false;
            for (int[] center : centers) {
                int dx = center[0] - x;
                int dy = center[1] - y;
                if (dx * dx + dy * dy < minDistance * minDistance) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                centers.add(new int[]{x, y});
            }
        }

        if (centers.size() == 1) {
            centers.add(new int[]{width / 4, (height * 3) / 4});
            centers.add(new int[]{(width * 3) / 4, height / 4});
        }
        return centers;
    }

    private boolean isValidSettlementCenter(int centerX, int centerY) {
        for (int x = centerX - 4; x <= centerX + 4; x++) {
            for (int y = centerY - 4; y <= centerY + 4; y++) {
                MapTile tile = generatedMap.getTile(x, y);
                if (tile == null || !tile.isWalkable() || tile.getType() == TileType.WATER || tile.getType() == TileType.MAGMA) {
                    return false;
                }
                if (tile.getHeightLevel() > 4) {
                    return false;
                }
            }
        }
        return true;
    }

    private void createSettlement(int centerX, int centerY, Random random, boolean primary, int index) {
        clearArea(centerX, centerY, 4, TileType.GRASS, true);

        for (int x = centerX - 4; x <= centerX + 4; x++) {
            setTile(x, centerY, TileType.ROAD, true);
        }
        for (int y = centerY - 4; y <= centerY + 4; y++) {
            setTile(centerX, y, TileType.ROAD, true);
        }

        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int y = centerY - 1; y <= centerY + 1; y++) {
                setTile(x, y, primary ? TileType.SPAWN : TileType.ROAD, true);
            }
        }

        generatedMap.addSpawnPoint(tileCenter(centerX, centerY));

        String[] buildingTypes = primary
                ? new String[] {"CITY_HALL", "CITY_SHOP", "CITY_HOUSE_TALL", "CITY_HOUSE_SMALL", "CITY_HOUSE_SMALL", "CITY_WAREHOUSE"}
                : new String[] {"CITY_SHOP", "CITY_HOUSE_SMALL", "CITY_HOUSE_TALL", "CITY_HOUSE_SMALL"};
        int[][] offsets = primary
                ? new int[][] {{-3, 2}, {3, 2}, {-3, -2}, {3, -2}, {-1, 4}, {1, -4}}
                : new int[][] {{-2, 2}, {2, 2}, {-2, -2}, {2, -2}};

        for (int i = 0; i < offsets.length; i++) {
            int tileX = centerX + offsets[i][0];
            int tileY = centerY + offsets[i][1];
            if (isBuildableSceneryTile(tileX, tileY)) {
                placeSceneryBuilding("town_" + index + "_" + i, buildingTypes[i % buildingTypes.length], tileX, tileY);
            }
        }

        if (primary) {
            int statueX = centerX + 4;
            int statueY = centerY;
            if (isBuildableSceneryTile(statueX, statueY)) {
                placeSceneryBuilding("town_" + index + "_generator", "GENERATOR", statueX, statueY);
            }
        }

        int treeAttempts = primary ? 6 : 4;
        for (int i = 0; i < treeAttempts; i++) {
            int tx = centerX - 5 + random.nextInt(11);
            int ty = centerY - 5 + random.nextInt(11);
            MapTile tile = generatedMap.getTile(tx, ty);
            if (tile != null && tile.getType() == TileType.GRASS && !occupiedTiles.containsKey(tileKey(tx, ty))) {
                tile.setType(TileType.FOREST);
                tile.setWalkable(true);
            }
        }
    }


    private void spawnStarterWorldItems(long seed) {
        Random random = new Random(seed ^ 0x17EAD10CL);
        List<Vector2f> spawnPoints = generatedMap.getSpawnPoints();
        if (spawnPoints.isEmpty()) {
            dropWorldItem(ItemCatalog.createStack(ItemCatalog.WOOD, 8), 2, 2);
            return;
        }

        String[] itemTypes = new String[] {
                ItemCatalog.WOOD,
                ItemCatalog.STONE,
                ItemCatalog.FIBER,
                ItemCatalog.BERRY,
                ItemCatalog.WOOD,
                ItemCatalog.STONE
        };

        for (int i = 0; i < itemTypes.length; i++) {
            Vector2f spawn = spawnPoints.get(i % spawnPoints.size());
            int centerX = (int) Math.floor(spawn.getX() / generatedMap.getTileSize());
            int centerY = (int) Math.floor(spawn.getY() / generatedMap.getTileSize());
            int tileX = centerX + random.nextInt(7) - 3;
            int tileY = centerY + random.nextInt(7) - 3;

            MapTile tile = generatedMap.getTile(tileX, tileY);
            if (tile == null || !tile.isWalkable()) {
                tileX = centerX;
                tileY = centerY;
            }

            int quantity = ItemCatalog.BERRY.equals(itemTypes[i]) ? 3 : 6 + random.nextInt(7);
            dropWorldItem(ItemCatalog.createStack(itemTypes[i], quantity), tileX, tileY);
        }
    }

    private void connectSettlements(int startX, int startY, int endX, int endY) {
        List<int[]> path = findRoadPath(startX, startY, endX, endY);
        if (path.isEmpty()) {
            int x = startX;
            int y = startY;
            while (x != endX) {
                setRoadTile(x, y);
                x += Integer.compare(endX, x);
            }
            while (y != endY) {
                setRoadTile(x, y);
                y += Integer.compare(endY, y);
            }
            setRoadTile(endX, endY);
            return;
        }

        for (int[] point : path) {
            setRoadTile(point[0], point[1]);
        }
    }

    private List<int[]> findRoadPath(int startX, int startY, int endX, int endY) {
        PriorityQueue<RoadNode> open = new PriorityQueue<>((a, b) -> Float.compare(a.priority, b.priority));
        Map<String, String> cameFrom = new HashMap<>();
        Map<String, Float> costSoFar = new HashMap<>();
        Set<String> closed = new HashSet<>();

        String startKey = tileKey(startX, startY);
        String endKey = tileKey(endX, endY);
        open.add(new RoadNode(startX, startY, 0f));
        costSoFar.put(startKey, 0f);

        int maxVisited = Math.max(4096, Math.min(60000, generatedMap.getWidth() * generatedMap.getHeight() / 3));
        int visited = 0;
        int[][] directions = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!open.isEmpty() && visited++ < maxVisited) {
            RoadNode current = open.poll();
            String currentKey = tileKey(current.x, current.y);
            if (!closed.add(currentKey)) {
                continue;
            }
            if (currentKey.equals(endKey)) {
                return reconstructRoadPath(cameFrom, startKey, endKey);
            }

            MapTile currentTile = generatedMap.getTile(current.x, current.y);
            for (int[] direction : directions) {
                int nx = current.x + direction[0];
                int ny = current.y + direction[1];
                MapTile nextTile = generatedMap.getTile(nx, ny);
                if (nextTile == null || nextTile.getType() == TileType.WALL || nextTile.getType() == TileType.MAGMA || nextTile.getType() == TileType.CLIFF) {
                    continue;
                }
                int heightDiff = currentTile == null ? 0 : Math.abs(nextTile.getHeightLevel() - currentTile.getHeightLevel());
                if (heightDiff > 1) {
                    continue;
                }
                String nextKey = tileKey(nx, ny);
                float terrainCost = nextTile.getType() == TileType.WATER ? 7f : nextTile.getType() == TileType.FOREST ? 2.3f : 1f;
                float newCost = costSoFar.get(currentKey) + terrainCost + heightDiff * 0.8f;
                if (!costSoFar.containsKey(nextKey) || newCost < costSoFar.get(nextKey)) {
                    costSoFar.put(nextKey, newCost);
                    float priority = newCost + Math.abs(endX - nx) + Math.abs(endY - ny);
                    open.add(new RoadNode(nx, ny, priority));
                    cameFrom.put(nextKey, currentKey);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<int[]> reconstructRoadPath(Map<String, String> cameFrom, String startKey, String endKey) {
        List<int[]> path = new ArrayList<>();
        String current = endKey;
        while (current != null && !current.equals(startKey)) {
            int comma = current.indexOf(',');
            if (comma < 0) {
                return Collections.emptyList();
            }
            path.add(new int[]{
                    Integer.parseInt(current.substring(0, comma)),
                    Integer.parseInt(current.substring(comma + 1))
            });
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private void setRoadTile(int tileX, int tileY) {
        MapTile tile = generatedMap.getTile(tileX, tileY);
        if (tile == null || tile.getType() == TileType.WALL || tile.getType() == TileType.MAGMA) {
            return;
        }
        tile.setType(TileType.ROAD);
        tile.setBiome(BiomeType.CITY);
        tile.setWalkable(true);
        tile.setResourceAmount(0);
    }

    private boolean isBuildableSceneryTile(int tileX, int tileY) {
        MapTile tile = generatedMap.getTile(tileX, tileY);
        if (tile == null) {
            return false;
        }
        if (!tile.isWalkable()) {
            return false;
        }
        TileType type = tile.getType();
        return (type == TileType.GRASS || type == TileType.ROAD || type == TileType.SPAWN)
                && !occupiedTiles.containsKey(tileKey(tileX, tileY));
    }

    private void placeSceneryBuilding(String ownerId, String buildingType, int tileX, int tileY) {
        EntityId id = EntityId.random();
        EntityState state = new EntityState(
                id,
                EntityType.BUILDING,
                ownerId,
                tileCenter(tileX, tileY),
                100f, 100f,
                0f, 0f
        );
        state.setBuildingType(buildingType);
        state.setTileX(tileX);
        state.setTileY(tileY);
        entities.put(id, state);
        occupiedTiles.put(tileKey(tileX, tileY), id);
    }

    private void clearArea(int centerX, int centerY, int radius, TileType type, boolean walkable) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                setTile(x, y, type, walkable);
            }
        }
    }

    private void setTile(int tileX, int tileY, TileType type, boolean walkable) {
        MapTile tile = generatedMap.getTile(tileX, tileY);
        if (tile == null || tile.getType() == TileType.WALL) {
            return;
        }
        tile.setType(type);
        tile.setWalkable(walkable);
        if (type == TileType.ROAD || type == TileType.SPAWN) {
            tile.setBiome(BiomeType.CITY);
            tile.setResourceAmount(0);
        }
    }

    private Vector2f tileCenter(int tileX, int tileY) {
        float tileSize = generatedMap == null ? DEFAULT_TILE_SIZE : generatedMap.getTileSize();
        float worldX = tileX * tileSize + tileSize * 0.5f;
        float worldY = tileY * tileSize + tileSize * 0.5f;
        return new Vector2f(worldX, worldY);
    }

    private static final class RoadNode {
        private final int x;
        private final int y;
        private final float priority;

        private RoadNode(int x, int y, float priority) {
            this.x = x;
            this.y = y;
            this.priority = priority;
        }
    }

    private boolean isTileBlocked(int fromTileX, int fromTileY, int tileX, int tileY) {
        MapTile target = generatedMap.getTile(tileX, tileY);
        if (target == null || !target.isWalkable()) {
            return true;
        }

        // Height levels are currently a generation/rendering layer.
        // Until explicit ramp/cliff-edge tiles are rendered, height differences must not create
        // invisible collision lines on otherwise normal grass/road/biome tiles.
        return occupiedTiles.containsKey(tileKey(tileX, tileY));
    }

    private String tileKey(int x, int y) {
        return x + "," + y;
    }

    private Vector2f getDefaultSpawnPoint() {
        if (!generatedMap.getSpawnPoints().isEmpty()) {
            return generatedMap.getSpawnPoints().get(0);
        }
        return new Vector2f(0.5f, 0.5f);
    }
}
