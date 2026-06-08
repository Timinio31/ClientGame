package com.tim.game.shared.config;

/**
 * Server-authoritative world/game configuration.
 *
 * The same object is used in three places:
 * - client profile JSON, so a server/world preset can be saved and opened again
 * - server JSON, so the authoritative game rules are loaded at server startup
 * - MAP_INIT payload, so every client knows which local controls/UI should be enabled
 */
public class WorldSettings {

    public static final String MAP_MODE_PROCEDURAL = "PROCEDURAL";
    public static final String MAP_MODE_FILE = "FILE";

    private String worldId = "default-world";
    private String worldName = "Default World";
    private String description = "Default configurable sandbox world.";
    private String gameMode = "SANDBOX";
    private String worldSaveDirectory = "";
    private String createdAt = "";
    private String lastPlayedAt = "";

    private String mapMode = MAP_MODE_PROCEDURAL;
    private String mapId = "default-procedural";
    private String mapFile = "";
    private int mapWidth = 1024;
    private int mapHeight = 1024;
    private float tileSize = 1.0f;
    private long seed = 0L;
    private boolean randomizeSeed = false;
    private boolean saveGeneratedMapOnStart = true;
    private boolean chunkStreamingEnabled = true;
    private int chunkSize = 32;
    private int mapInitFullTileLimit = 16384;
    private int maxHeightLevel = 5;

    private boolean movementEnabled = true;
    private float movementRepeatIntervalSeconds = 0.15f;
    private int movementCooldownTicks = 3;

    private boolean playerInteractionEnabled = true;
    private boolean inventoryEnabled = true;
    private boolean buildingEnabled = true;
    private boolean craftingEnabled = true;
    private boolean craftingMenuEnabled = true;
    private boolean inventoryCraftingEnabled = true;
    private boolean workstationCraftingEnabled = true;
    private boolean toolCraftingEnabled = true;
    private boolean craftingQualityEnabled = false;
    private boolean craftingFailureEnabled = false;
    private boolean combatEnabled = true;
    private boolean teamCallEnabled = false;
    private boolean companionsEnabled = false;
    private boolean groupsEnabled = false;

    private boolean bibblesEnabled = true;
    private boolean bibblesFrozenWhenDisabled = true;
    private boolean bibbleSpawningEnabled = true;
    private boolean bibbleCapturingEnabled = true;
    private boolean bibbleTradingEnabled = true;
    private boolean bibbleWorkstationAutomationEnabled = true;
    private boolean bibbleCombatEnabled = true;
    private boolean bibbleFriendlyFireEnabled = false;
    private boolean bibblePermanentDeathEnabled = false;
    private boolean bibbleFreeRoamEnabled = true;
    private boolean bibbleTerminalEnabled = true;
    private boolean bibbleAreasEnabled = true;
    private boolean starterBibblesEnabled = false;
    private int maxActiveBibbles = 3;
    private int maxStoredBibbles = 64;
    private int maxBibblesPerWorkstation = 1;
    private int bibbleFollowTeleportDistance = 10;

    private boolean worldItemsEnabled = true;
    private boolean mapEditingEnabled = false;

    private boolean settlementsEnabled = true;
    private boolean starterItemsEnabled = true;

    private boolean playerRespawnEnabled = true;
    private boolean playerDeathChestEnabled = true;
    private boolean playerDropsInventoryOnDeath = true;
    private boolean playerDropsToolsOnDeath = false;
    private boolean playerDropsBibblesOnDeath = false;
    private boolean playerDropsWorkstationsOnDeath = false;
    private int playerDeathInventoryDropPercent = 50;

    private int maxPlayers = 16;
    private int maxGroupSize = 4;

    public WorldSettings() {
    }

    public static WorldSettings defaults() {
        return new WorldSettings().normalizedCopy();
    }

    public WorldSettings normalizedCopy() {
        WorldSettings copy = new WorldSettings();
        copy.setWorldId(safeId(defaultIfBlank(worldId, mapId)));
        copy.setWorldName(defaultIfBlank(worldName, "Default World"));
        copy.setDescription(description == null ? "" : description.trim());
        copy.setGameMode(defaultIfBlank(gameMode, "SANDBOX").toUpperCase());
        copy.setWorldSaveDirectory(worldSaveDirectory == null ? "" : worldSaveDirectory.trim());
        copy.setCreatedAt(createdAt == null ? "" : createdAt.trim());
        copy.setLastPlayedAt(lastPlayedAt == null ? "" : lastPlayedAt.trim());
        copy.setMapMode(normalizeMapMode(mapMode));
        copy.setMapId(defaultIfBlank(mapId, "default-procedural"));
        copy.setMapFile(mapFile == null ? "" : mapFile.trim());
        copy.setMapWidth(clamp(mapWidth, 8, 8192));
        copy.setMapHeight(clamp(mapHeight, 8, 8192));
        copy.setTileSize(tileSize > 0f ? tileSize : 1.0f);
        copy.setSeed(seed);
        copy.setRandomizeSeed(randomizeSeed);
        copy.setSaveGeneratedMapOnStart(saveGeneratedMapOnStart);
        copy.setChunkStreamingEnabled(chunkStreamingEnabled);
        copy.setChunkSize(clamp(chunkSize, 8, 128));
        copy.setMapInitFullTileLimit(clamp(mapInitFullTileLimit, 0, 65536));
        copy.setMaxHeightLevel(clamp(maxHeightLevel, 1, 9));

        copy.setMovementEnabled(movementEnabled);
        copy.setMovementRepeatIntervalSeconds(clampFloat(movementRepeatIntervalSeconds, 0.05f, 2.0f));
        copy.setMovementCooldownTicks(clamp(movementCooldownTicks, 1, 120));

        copy.setPlayerInteractionEnabled(playerInteractionEnabled);
        copy.setInventoryEnabled(inventoryEnabled);
        copy.setBuildingEnabled(buildingEnabled);
        copy.setCraftingEnabled(craftingEnabled);
        copy.setCraftingMenuEnabled(craftingMenuEnabled && craftingEnabled);
        copy.setInventoryCraftingEnabled(inventoryCraftingEnabled && craftingEnabled);
        copy.setWorkstationCraftingEnabled(workstationCraftingEnabled && craftingEnabled);
        copy.setToolCraftingEnabled(toolCraftingEnabled && craftingEnabled);
        copy.setCraftingQualityEnabled(craftingQualityEnabled && craftingEnabled);
        copy.setCraftingFailureEnabled(craftingFailureEnabled && craftingEnabled);
        copy.setCombatEnabled(combatEnabled);
        copy.setTeamCallEnabled(teamCallEnabled);
        copy.setCompanionsEnabled(companionsEnabled);
        copy.setGroupsEnabled(groupsEnabled);

        copy.setBibblesEnabled(bibblesEnabled);
        copy.setBibblesFrozenWhenDisabled(bibblesFrozenWhenDisabled);
        copy.setBibbleSpawningEnabled(bibbleSpawningEnabled && bibblesEnabled);
        copy.setBibbleCapturingEnabled(bibbleCapturingEnabled && bibblesEnabled);
        copy.setBibbleTradingEnabled(bibbleTradingEnabled && bibblesEnabled);
        copy.setBibbleWorkstationAutomationEnabled(bibbleWorkstationAutomationEnabled && bibblesEnabled);
        copy.setBibbleCombatEnabled(bibbleCombatEnabled && bibblesEnabled);
        copy.setBibbleFriendlyFireEnabled(bibbleFriendlyFireEnabled && bibbleCombatEnabled && bibblesEnabled);
        copy.setBibblePermanentDeathEnabled(bibblePermanentDeathEnabled && bibbleCombatEnabled && bibblesEnabled);
        copy.setBibbleFreeRoamEnabled(bibbleFreeRoamEnabled && bibblesEnabled);
        copy.setBibbleTerminalEnabled(bibbleTerminalEnabled && bibblesEnabled);
        copy.setBibbleAreasEnabled(bibbleAreasEnabled && bibblesEnabled);
        copy.setStarterBibblesEnabled(starterBibblesEnabled && bibblesEnabled);
        copy.setMaxActiveBibbles(clamp(maxActiveBibbles, 0, 3));
        copy.setMaxStoredBibbles(clamp(maxStoredBibbles, 0, 999));
        copy.setMaxBibblesPerWorkstation(clamp(maxBibblesPerWorkstation, 0, 1));
        copy.setBibbleFollowTeleportDistance(clamp(bibbleFollowTeleportDistance, 3, 64));

        copy.setWorldItemsEnabled(worldItemsEnabled);
        copy.setMapEditingEnabled(mapEditingEnabled);

        copy.setSettlementsEnabled(settlementsEnabled);
        copy.setStarterItemsEnabled(starterItemsEnabled);

        copy.setPlayerRespawnEnabled(playerRespawnEnabled);
        copy.setPlayerDeathChestEnabled(playerDeathChestEnabled);
        copy.setPlayerDropsInventoryOnDeath(playerDropsInventoryOnDeath);
        copy.setPlayerDropsToolsOnDeath(playerDropsToolsOnDeath);
        copy.setPlayerDropsBibblesOnDeath(playerDropsBibblesOnDeath);
        copy.setPlayerDropsWorkstationsOnDeath(playerDropsWorkstationsOnDeath);
        copy.setPlayerDeathInventoryDropPercent(clamp(playerDeathInventoryDropPercent, 0, 100));

        copy.setMaxPlayers(clamp(maxPlayers, 1, 256));
        copy.setMaxGroupSize(clamp(maxGroupSize, 1, 64));
        return copy;
    }

    public static String normalizeMapMode(String value) {
        if (value == null || value.isBlank()) {
            return MAP_MODE_PROCEDURAL;
        }
        String normalized = value.trim().toUpperCase();
        if (MAP_MODE_FILE.equals(normalized)) {
            return MAP_MODE_FILE;
        }
        return MAP_MODE_PROCEDURAL;
    }

    public boolean usesFileMap() {
        return MAP_MODE_FILE.equals(normalizeMapMode(mapMode));
    }

    public long resolveSeed(String roomId) {
        if (randomizeSeed) {
            return System.currentTimeMillis();
        }
        if (seed != 0L) {
            return seed;
        }
        String seedSource = defaultIfBlank(worldId, defaultIfBlank(mapId, roomId));
        return Integer.toUnsignedLong(defaultIfBlank(seedSource, "1").hashCode());
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String safeId(String value) {
        String source = defaultIfBlank(value, "default-world").toLowerCase();
        String safe = source.replaceAll("[^a-z0-9._-]", "-").replaceAll("-+", "-");
        return safe.isBlank() || "-".equals(safe) ? "default-world" : safe;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public String getWorldId() {
        return worldId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public String getWorldSaveDirectory() {
        return worldSaveDirectory;
    }

    public void setWorldSaveDirectory(String worldSaveDirectory) {
        this.worldSaveDirectory = worldSaveDirectory;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastPlayedAt() {
        return lastPlayedAt;
    }

    public void setLastPlayedAt(String lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public String getMapMode() {
        return mapMode;
    }

    public void setMapMode(String mapMode) {
        this.mapMode = mapMode;
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    public String getMapFile() {
        return mapFile;
    }

    public void setMapFile(String mapFile) {
        this.mapFile = mapFile;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public void setMapWidth(int mapWidth) {
        this.mapWidth = mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public void setMapHeight(int mapHeight) {
        this.mapHeight = mapHeight;
    }

    public float getTileSize() {
        return tileSize;
    }

    public void setTileSize(float tileSize) {
        this.tileSize = tileSize;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public boolean isRandomizeSeed() {
        return randomizeSeed;
    }

    public void setRandomizeSeed(boolean randomizeSeed) {
        this.randomizeSeed = randomizeSeed;
    }

    public boolean isSaveGeneratedMapOnStart() {
        return saveGeneratedMapOnStart;
    }

    public void setSaveGeneratedMapOnStart(boolean saveGeneratedMapOnStart) {
        this.saveGeneratedMapOnStart = saveGeneratedMapOnStart;
    }

    public boolean isChunkStreamingEnabled() {
        return chunkStreamingEnabled;
    }

    public void setChunkStreamingEnabled(boolean chunkStreamingEnabled) {
        this.chunkStreamingEnabled = chunkStreamingEnabled;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getMapInitFullTileLimit() {
        return mapInitFullTileLimit;
    }

    public void setMapInitFullTileLimit(int mapInitFullTileLimit) {
        this.mapInitFullTileLimit = mapInitFullTileLimit;
    }

    public int getMaxHeightLevel() {
        return maxHeightLevel;
    }

    public void setMaxHeightLevel(int maxHeightLevel) {
        this.maxHeightLevel = maxHeightLevel;
    }

    public boolean isMovementEnabled() {
        return movementEnabled;
    }

    public void setMovementEnabled(boolean movementEnabled) {
        this.movementEnabled = movementEnabled;
    }

    public float getMovementRepeatIntervalSeconds() {
        return movementRepeatIntervalSeconds;
    }

    public void setMovementRepeatIntervalSeconds(float movementRepeatIntervalSeconds) {
        this.movementRepeatIntervalSeconds = movementRepeatIntervalSeconds;
    }

    public int getMovementCooldownTicks() {
        return movementCooldownTicks;
    }

    public void setMovementCooldownTicks(int movementCooldownTicks) {
        this.movementCooldownTicks = movementCooldownTicks;
    }

    public boolean isPlayerInteractionEnabled() {
        return playerInteractionEnabled;
    }

    public void setPlayerInteractionEnabled(boolean playerInteractionEnabled) {
        this.playerInteractionEnabled = playerInteractionEnabled;
    }

    public boolean isInventoryEnabled() {
        return inventoryEnabled;
    }

    public void setInventoryEnabled(boolean inventoryEnabled) {
        this.inventoryEnabled = inventoryEnabled;
    }

    public boolean isBuildingEnabled() {
        return buildingEnabled;
    }

    public void setBuildingEnabled(boolean buildingEnabled) {
        this.buildingEnabled = buildingEnabled;
    }

    public boolean isCraftingEnabled() {
        return craftingEnabled;
    }

    public void setCraftingEnabled(boolean craftingEnabled) {
        this.craftingEnabled = craftingEnabled;
    }

    public boolean isCraftingMenuEnabled() {
        return craftingMenuEnabled;
    }

    public void setCraftingMenuEnabled(boolean craftingMenuEnabled) {
        this.craftingMenuEnabled = craftingMenuEnabled;
    }

    public boolean isInventoryCraftingEnabled() {
        return inventoryCraftingEnabled;
    }

    public void setInventoryCraftingEnabled(boolean inventoryCraftingEnabled) {
        this.inventoryCraftingEnabled = inventoryCraftingEnabled;
    }

    public boolean isWorkstationCraftingEnabled() {
        return workstationCraftingEnabled;
    }

    public void setWorkstationCraftingEnabled(boolean workstationCraftingEnabled) {
        this.workstationCraftingEnabled = workstationCraftingEnabled;
    }

    public boolean isToolCraftingEnabled() {
        return toolCraftingEnabled;
    }

    public void setToolCraftingEnabled(boolean toolCraftingEnabled) {
        this.toolCraftingEnabled = toolCraftingEnabled;
    }

    public boolean isCraftingQualityEnabled() {
        return craftingQualityEnabled;
    }

    public void setCraftingQualityEnabled(boolean craftingQualityEnabled) {
        this.craftingQualityEnabled = craftingQualityEnabled;
    }

    public boolean isCraftingFailureEnabled() {
        return craftingFailureEnabled;
    }

    public void setCraftingFailureEnabled(boolean craftingFailureEnabled) {
        this.craftingFailureEnabled = craftingFailureEnabled;
    }

    public boolean isCombatEnabled() {
        return combatEnabled;
    }

    public void setCombatEnabled(boolean combatEnabled) {
        this.combatEnabled = combatEnabled;
    }

    public boolean isTeamCallEnabled() {
        return teamCallEnabled;
    }

    public void setTeamCallEnabled(boolean teamCallEnabled) {
        this.teamCallEnabled = teamCallEnabled;
    }

    public boolean isCompanionsEnabled() {
        return companionsEnabled;
    }

    public void setCompanionsEnabled(boolean companionsEnabled) {
        this.companionsEnabled = companionsEnabled;
    }

    public boolean isGroupsEnabled() {
        return groupsEnabled;
    }

    public void setGroupsEnabled(boolean groupsEnabled) {
        this.groupsEnabled = groupsEnabled;
    }

    public boolean isBibblesEnabled() {
        return bibblesEnabled;
    }

    public void setBibblesEnabled(boolean bibblesEnabled) {
        this.bibblesEnabled = bibblesEnabled;
    }

    public boolean isBibblesFrozenWhenDisabled() {
        return bibblesFrozenWhenDisabled;
    }

    public void setBibblesFrozenWhenDisabled(boolean bibblesFrozenWhenDisabled) {
        this.bibblesFrozenWhenDisabled = bibblesFrozenWhenDisabled;
    }

    public boolean isBibbleSpawningEnabled() {
        return bibbleSpawningEnabled;
    }

    public void setBibbleSpawningEnabled(boolean bibbleSpawningEnabled) {
        this.bibbleSpawningEnabled = bibbleSpawningEnabled;
    }

    public boolean isBibbleCapturingEnabled() {
        return bibbleCapturingEnabled;
    }

    public void setBibbleCapturingEnabled(boolean bibbleCapturingEnabled) {
        this.bibbleCapturingEnabled = bibbleCapturingEnabled;
    }

    public boolean isBibbleTradingEnabled() {
        return bibbleTradingEnabled;
    }

    public void setBibbleTradingEnabled(boolean bibbleTradingEnabled) {
        this.bibbleTradingEnabled = bibbleTradingEnabled;
    }

    public boolean isBibbleWorkstationAutomationEnabled() {
        return bibbleWorkstationAutomationEnabled;
    }

    public void setBibbleWorkstationAutomationEnabled(boolean bibbleWorkstationAutomationEnabled) {
        this.bibbleWorkstationAutomationEnabled = bibbleWorkstationAutomationEnabled;
    }

    public boolean isBibbleCombatEnabled() {
        return bibbleCombatEnabled;
    }

    public void setBibbleCombatEnabled(boolean bibbleCombatEnabled) {
        this.bibbleCombatEnabled = bibbleCombatEnabled;
    }

    public boolean isBibbleFriendlyFireEnabled() {
        return bibbleFriendlyFireEnabled;
    }

    public void setBibbleFriendlyFireEnabled(boolean bibbleFriendlyFireEnabled) {
        this.bibbleFriendlyFireEnabled = bibbleFriendlyFireEnabled;
    }

    public boolean isBibblePermanentDeathEnabled() {
        return bibblePermanentDeathEnabled;
    }

    public void setBibblePermanentDeathEnabled(boolean bibblePermanentDeathEnabled) {
        this.bibblePermanentDeathEnabled = bibblePermanentDeathEnabled;
    }

    public boolean isBibbleFreeRoamEnabled() {
        return bibbleFreeRoamEnabled;
    }

    public void setBibbleFreeRoamEnabled(boolean bibbleFreeRoamEnabled) {
        this.bibbleFreeRoamEnabled = bibbleFreeRoamEnabled;
    }

    public boolean isBibbleTerminalEnabled() {
        return bibbleTerminalEnabled;
    }

    public void setBibbleTerminalEnabled(boolean bibbleTerminalEnabled) {
        this.bibbleTerminalEnabled = bibbleTerminalEnabled;
    }

    public boolean isBibbleAreasEnabled() {
        return bibbleAreasEnabled;
    }

    public void setBibbleAreasEnabled(boolean bibbleAreasEnabled) {
        this.bibbleAreasEnabled = bibbleAreasEnabled;
    }

    public boolean isStarterBibblesEnabled() {
        return starterBibblesEnabled;
    }

    public void setStarterBibblesEnabled(boolean starterBibblesEnabled) {
        this.starterBibblesEnabled = starterBibblesEnabled;
    }

    public int getMaxActiveBibbles() {
        return maxActiveBibbles;
    }

    public void setMaxActiveBibbles(int maxActiveBibbles) {
        this.maxActiveBibbles = maxActiveBibbles;
    }

    public int getMaxStoredBibbles() {
        return maxStoredBibbles;
    }

    public void setMaxStoredBibbles(int maxStoredBibbles) {
        this.maxStoredBibbles = maxStoredBibbles;
    }

    public int getMaxBibblesPerWorkstation() {
        return maxBibblesPerWorkstation;
    }

    public void setMaxBibblesPerWorkstation(int maxBibblesPerWorkstation) {
        this.maxBibblesPerWorkstation = maxBibblesPerWorkstation;
    }

    public int getBibbleFollowTeleportDistance() {
        return bibbleFollowTeleportDistance;
    }

    public void setBibbleFollowTeleportDistance(int bibbleFollowTeleportDistance) {
        this.bibbleFollowTeleportDistance = bibbleFollowTeleportDistance;
    }

    public boolean isWorldItemsEnabled() {
        return worldItemsEnabled;
    }

    public void setWorldItemsEnabled(boolean worldItemsEnabled) {
        this.worldItemsEnabled = worldItemsEnabled;
    }

    public boolean isMapEditingEnabled() {
        return mapEditingEnabled;
    }

    public void setMapEditingEnabled(boolean mapEditingEnabled) {
        this.mapEditingEnabled = mapEditingEnabled;
    }

    public boolean isSettlementsEnabled() {
        return settlementsEnabled;
    }

    public void setSettlementsEnabled(boolean settlementsEnabled) {
        this.settlementsEnabled = settlementsEnabled;
    }

    public boolean isStarterItemsEnabled() {
        return starterItemsEnabled;
    }

    public void setStarterItemsEnabled(boolean starterItemsEnabled) {
        this.starterItemsEnabled = starterItemsEnabled;
    }


    public boolean isPlayerRespawnEnabled() {
        return playerRespawnEnabled;
    }

    public void setPlayerRespawnEnabled(boolean playerRespawnEnabled) {
        this.playerRespawnEnabled = playerRespawnEnabled;
    }

    public boolean isPlayerDeathChestEnabled() {
        return playerDeathChestEnabled;
    }

    public void setPlayerDeathChestEnabled(boolean playerDeathChestEnabled) {
        this.playerDeathChestEnabled = playerDeathChestEnabled;
    }

    public boolean isPlayerDropsInventoryOnDeath() {
        return playerDropsInventoryOnDeath;
    }

    public void setPlayerDropsInventoryOnDeath(boolean playerDropsInventoryOnDeath) {
        this.playerDropsInventoryOnDeath = playerDropsInventoryOnDeath;
    }

    public boolean isPlayerDropsToolsOnDeath() {
        return playerDropsToolsOnDeath;
    }

    public void setPlayerDropsToolsOnDeath(boolean playerDropsToolsOnDeath) {
        this.playerDropsToolsOnDeath = playerDropsToolsOnDeath;
    }

    public boolean isPlayerDropsBibblesOnDeath() {
        return playerDropsBibblesOnDeath;
    }

    public void setPlayerDropsBibblesOnDeath(boolean playerDropsBibblesOnDeath) {
        this.playerDropsBibblesOnDeath = playerDropsBibblesOnDeath;
    }

    public boolean isPlayerDropsWorkstationsOnDeath() {
        return playerDropsWorkstationsOnDeath;
    }

    public void setPlayerDropsWorkstationsOnDeath(boolean playerDropsWorkstationsOnDeath) {
        this.playerDropsWorkstationsOnDeath = playerDropsWorkstationsOnDeath;
    }

    public int getPlayerDeathInventoryDropPercent() {
        return playerDeathInventoryDropPercent;
    }

    public void setPlayerDeathInventoryDropPercent(int playerDeathInventoryDropPercent) {
        this.playerDeathInventoryDropPercent = playerDeathInventoryDropPercent;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getMaxGroupSize() {
        return maxGroupSize;
    }

    public void setMaxGroupSize(int maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
    }

    @Override
    public String toString() {
        return "WorldSettings{" +
                "worldId='" + worldId + '\'' +
                ", worldName='" + worldName + '\'' +
                ", gameMode='" + gameMode + '\'' +
                ", mapMode='" + mapMode + '\'' +
                ", mapId='" + mapId + '\'' +
                ", mapWidth=" + mapWidth +
                ", mapHeight=" + mapHeight +
                ", seed=" + seed +
                ", chunkStreamingEnabled=" + chunkStreamingEnabled +
                ", chunkSize=" + chunkSize +
                ", maxHeightLevel=" + maxHeightLevel +
                ", movementEnabled=" + movementEnabled +
                ", inventoryEnabled=" + inventoryEnabled +
                ", buildingEnabled=" + buildingEnabled +
                ", craftingEnabled=" + craftingEnabled +
                ", craftingMenuEnabled=" + craftingMenuEnabled +
                ", inventoryCraftingEnabled=" + inventoryCraftingEnabled +
                ", combatEnabled=" + combatEnabled +
                ", bibblesEnabled=" + bibblesEnabled +
                ", playerRespawnEnabled=" + playerRespawnEnabled +
                ", playerDeathInventoryDropPercent=" + playerDeathInventoryDropPercent +
                ", maxActiveBibbles=" + maxActiveBibbles +
                ", bibbleWorkstationAutomationEnabled=" + bibbleWorkstationAutomationEnabled +
                '}';
    }
}
