package com.tim.game.server.world;

import com.tim.game.server.world.map.BasicProceduralMapGenerator;
import com.tim.game.server.world.map.GeneratedMap;
import com.tim.game.server.world.map.MapGenerator;
import com.tim.game.server.world.map.MapTile;
import com.tim.game.server.world.inventory.PlayerInventory;
import com.tim.game.shared.DTOs.update.BuildingStateDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.TileStateDto;
import com.tim.game.shared.DTOs.update.ItemStackDto;
import com.tim.game.shared.DTOs.update.WorldItemStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;
import com.tim.game.shared.model.EntityId;
import com.tim.game.shared.model.EntityType;
import com.tim.game.shared.model.Vector2f;
import com.tim.game.shared.inventory.ItemCatalog;
import com.tim.game.shared.world.TileType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WorldState {

    private static final float TILE_SIZE = 1.0f;

    private final String roomId;
    private long tick;

    private final Map<EntityId, EntityState> entities = new HashMap<>();
    private final Map<String, EntityId> playerEntities = new HashMap<>();
    private final Map<String, EntityId> occupiedTiles = new HashMap<>();
    private final Map<String, PlayerInventory> playerInventories = new HashMap<>();
    private final List<WorldItemStateDto> worldItems = new ArrayList<>();

    private final GeneratedMap generatedMap;
    private final MapGenerator mapGenerator = new BasicProceduralMapGenerator();

    public WorldState(String roomId) {
        this.roomId = roomId;
        this.tick = 0L;

        long seed = Integer.toUnsignedLong(roomId.hashCode());
        this.generatedMap = mapGenerator.generate(roomId, 32, 32, TILE_SIZE, seed);
        generateSettlements(seed);
        spawnStarterWorldItems(seed);

        DebugConfig.log(DebugCategory.MAP,
                "Generated world for room " + roomId
                        + " seed=" + generatedMap.getSeed()
                        + " size=" + generatedMap.getWidth() + "x" + generatedMap.getHeight()
                        + " spawns=" + generatedMap.getSpawnPoints().size()
                        + " scenicBuildings=" + buildBuildingStateList().size());
    }

    public void incrementTick() {
        tick++;
    }

    public long getTick() {
        return tick;
    }

    public String getRoomId() {
        return roomId;
    }

    public EntityId spawnPlayerForClient(String clientId, Vector2f startPosition) {
        EntityId id = EntityId.player(clientId);

        int startTileX = (int) Math.floor(startPosition.getX() / TILE_SIZE);
        int startTileY = (int) Math.floor(startPosition.getY() / TILE_SIZE);
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

        if (isTileBlocked(targetX, targetY)) {
            DebugConfig.log(DebugCategory.MOVEMENT,
                    "blocked client=" + clientId
                            + " from=(" + playerX + "," + playerY + ")"
                            + " to=(" + targetX + "," + targetY + ")"
                            + " type=" + targetTile.getType()
                            + " walkable=" + targetTile.isWalkable()
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

        EntityState state = new EntityState(
                id,
                EntityType.BUILDING,
                clientId,
                position,
                100f, 100f,
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


    public void selectInventorySlot(String clientId, int slotIndex) {
        getOrCreateInventory(clientId).selectSlot(slotIndex);
    }

    public boolean useSelectedInventoryItem(String clientId) {
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
        if (ItemCatalog.BERRY.equals(itemType)) {
            float healedHealth = Math.min(player.getMaxHealth(), player.getHealth() + 15f);
            if (healedHealth <= player.getHealth()) {
                return false;
            }

            player.setHealth(healedHealth);
            inventory.removeFromSlot(inventory.getSelectedSlot(), 1);
            return true;
        }

        return false;
    }

    public boolean dropSelectedInventoryItem(String clientId, int amount) {
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
        return dto;
    }

    public MapInitDto buildMapInit() {
        MapInitDto dto = new MapInitDto(
                generatedMap.getRoomId(),
                generatedMap.getWidth(),
                generatedMap.getHeight(),
                generatedMap.getTileSize(),
                generatedMap.getSeed()
        );

        for (int x = 0; x < generatedMap.getWidth(); x++) {
            for (int y = 0; y < generatedMap.getHeight(); y++) {
                MapTile tile = generatedMap.getTile(x, y);
                if (tile == null) {
                    continue;
                }

                dto.addTile(new TileStateDto(
                        tile.getX(),
                        tile.getY(),
                        tile.getType().name(),
                        tile.isWalkable()
                ));
            }
        }

        for (Vector2f spawnPoint : generatedMap.getSpawnPoints()) {
            dto.addSpawnPoint(spawnPoint);
        }

        return dto;
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
            PlayerInventory inventory = getOrCreateInventory(clientId);
            dto.setInventorySlots(inventory.toDtoSlots());
            dto.setSelectedInventorySlot(inventory.getSelectedSlot());
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

        int[][] centers = new int[][] {
                {generatedMap.getWidth() / 2, generatedMap.getHeight() / 2},
                {generatedMap.getWidth() / 4, (generatedMap.getHeight() * 3) / 4},
                {(generatedMap.getWidth() * 3) / 4, generatedMap.getHeight() / 4}
        };

        for (int i = 0; i < centers.length; i++) {
            int cx = centers[i][0];
            int cy = centers[i][1];
            createSettlement(cx, cy, random, i == 0, i);
        }

        connectSettlements(centers[0][0], centers[0][1], centers[1][0], centers[1][1]);
        connectSettlements(centers[0][0], centers[0][1], centers[2][0], centers[2][1]);
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
                ItemCatalog.BERRY,
                ItemCatalog.WOOD,
                ItemCatalog.STONE
        };

        for (int i = 0; i < itemTypes.length; i++) {
            Vector2f spawn = spawnPoints.get(i % spawnPoints.size());
            int centerX = (int) Math.floor(spawn.getX() / TILE_SIZE);
            int centerY = (int) Math.floor(spawn.getY() / TILE_SIZE);
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
        int x = startX;
        int y = startY;
        while (x != endX) {
            setTile(x, y, TileType.ROAD, true);
            x += Integer.compare(endX, x);
        }
        while (y != endY) {
            setTile(x, y, TileType.ROAD, true);
            y += Integer.compare(endY, y);
        }
        setTile(endX, endY, TileType.ROAD, true);
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
    }

    private Vector2f tileCenter(int tileX, int tileY) {
        float worldX = tileX * TILE_SIZE + TILE_SIZE * 0.5f;
        float worldY = tileY * TILE_SIZE + TILE_SIZE * 0.5f;
        return new Vector2f(worldX, worldY);
    }

    private boolean isTileBlocked(int tileX, int tileY) {
        if (!generatedMap.isWalkable(tileX, tileY)) {
            return true;
        }

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
