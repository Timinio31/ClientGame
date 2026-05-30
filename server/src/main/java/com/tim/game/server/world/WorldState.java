package com.tim.game.server.world;

import com.tim.game.server.world.map.BasicProceduralMapGenerator;
import com.tim.game.server.world.map.GeneratedMap;
import com.tim.game.server.world.map.MapGenerator;
import com.tim.game.server.world.map.MapTile;
import com.tim.game.shared.DTOs.update.BuildingStateDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.TileStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;
import com.tim.game.shared.model.EntityId;
import com.tim.game.shared.model.EntityType;
import com.tim.game.shared.model.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldState {

    private static final float TILE_SIZE = 1.0f;

    private final String roomId;
    private long tick;

    private final Map<EntityId, EntityState> entities = new HashMap<>();
    private final Map<String, EntityId> playerEntities = new HashMap<>();
    private final Map<String, EntityId> occupiedTiles = new HashMap<>();

    private final GeneratedMap generatedMap;
    private final MapGenerator mapGenerator = new BasicProceduralMapGenerator();

    public WorldState(String roomId) {
        this.roomId = roomId;
        this.tick = 0L;

        long seed = Integer.toUnsignedLong(roomId.hashCode());
        this.generatedMap = mapGenerator.generate(roomId, 32, 32, TILE_SIZE, seed);

        DebugConfig.log(DebugCategory.MAP,
                "Generated map for room " + roomId
                        + " seed=" + generatedMap.getSeed()
                        + " size=" + generatedMap.getWidth() + "x" + generatedMap.getHeight()
                        + " spawns=" + generatedMap.getSpawnPoints().size());
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

        DebugConfig.log(DebugCategory.BUILDING,
                "BUILD placed client=" + clientId
                        + " type=" + buildingType
                        + " tile=(" + tileX + "," + tileY + ")");

        return true;
    }

    /**
     * Dynamischer Snapshot. Die Map wird hier bewusst nicht mehr gesetzt.
     */
    public WorldSnapshotDto buildSnapshot() {
        return buildDynamicSnapshot(tick);
    }

    public WorldSnapshotDto buildDynamicSnapshot(long tick) {
        WorldSnapshotDto dto = new WorldSnapshotDto();
        dto.setRoomId(roomId);
        dto.setTick(tick);
        dto.setPlayers(buildPlayerStateList());
        dto.setBuildings(buildBuildingStateList());
        return dto;
    }

    /**
     * Einmalige statische Map-Initialisierung für einzelne Clients.
     */
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

            result.add(new PlayerStateDto(
                    state.getId().getValue(),
                    clientId,
                    state.getType(),
                    state.getPosition(),
                    state.getHealth(),
                    state.getMaxHealth(),
                    state.getStamina(),
                    state.getMaxStamina()
            ));
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
