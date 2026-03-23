package com.tim.game.server.world;

import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.model.EntityId;
import com.tim.game.shared.model.EntityType;
import com.tim.game.shared.model.Vector2f;
import com.tim.game.shared.DTOs.update.BuildingStateDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldState {

    private final String roomId;

    // Tick-Zähler für die Simulation (z.B. jeder GameLoop-Tick +1)
    private long tick;

    // Alle Entities in der Welt: EntityId -> EntityState
    private final Map<EntityId, EntityState> entities = new HashMap<>();

    // Mapping: clientId -> EntityId des Spieler-Avatars
    private final Map<String, EntityId> playerEntities = new HashMap<>();

    private static final float TILE_SIZE = 1.0f;

    // tileKey -> buildingEntityId
    private final Map<String, EntityId> occupiedTiles = new HashMap<>();

    public WorldState(String roomId) {
        this.roomId = roomId;
        this.tick = 0L;
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
        /**
     * Erzeugt eine neue Spieler-Entity für den gegebenen Client.
     * Startwerte sind erstmal hart gecodet – später konfigurierbar.
     */
    public EntityId spawnPlayerForClient(String clientId, Vector2f startPosition) {
        EntityId id = EntityId.player(clientId);

        // 1) Weltposition -> Tile
        int startTileX = (int)Math.floor(startPosition.getX() / TILE_SIZE);
        int startTileY = (int)Math.floor(startPosition.getY() / TILE_SIZE);

        // 2) Tile-Center -> Weltposition (snapped)
        Vector2f snappedStartPos = tileCenter(startTileX, startTileY);

        EntityState state = new EntityState(
                id,
                EntityType.PLAYER,
                clientId,
                snappedStartPos,
                100f, 100f,
                100f, 100f
        );

        // 3) Tile-Koordinaten im Player speichern
        state.setTileX(startTileX);
        state.setTileY(startTileY);

        entities.put(id, state);
        playerEntities.put(clientId, id);

        return id;
    }

    
    public EntityState getPlayerEntity(String clientId) {
        EntityId id = playerEntities.get(clientId);
        if (id == null) {
            return null;
        }
        return entities.get(id);
    }
    
    /**
     * Bewegt einen Spieler ein Stück in eine Richtung.
     * direction sollte normalisiert sein (Länge 1), speed in Einheiten pro Tick.
     */

    public void movePlayerStep(String clientId, int directionX, int directionY) {
        if (Math.abs(directionX) + Math.abs(directionY) != 1) return;

        EntityState player = getPlayerEntity(clientId);

        if (player == null) {
            spawnPlayerForClient(clientId, new Vector2f(0f, 0f)); // spawnt auf 0.5/0.5
            player = getPlayerEntity(clientId);
            if (player == null) return;
        }

        int playerX = (player.getTileX() != null) ? player.getTileX() : 0;
        int playerY = (player.getTileY() != null) ? player.getTileY() : 0;

        int newPlayerX = playerX + directionX;
        int newPlayerY = playerY + directionY;

        if (isTileBlocked(newPlayerX, newPlayerY)) return;

        player.setTileX(newPlayerX);
        player.setTileY(newPlayerY);
        player.setPosition(tileCenter(newPlayerX, newPlayerY));
    }

    /**
     * Erzeugt eine Liste von PlayerStateDto für alle Spieler in der Welt.
     */
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
                    state.getId().toString(),
                    clientId,
                    state.getType(),
                    state.getPosition(),
                    state.getHealth(),
                    state.getMaxHealth(),
                    state.getStamina(),
                    state.getMaxStamina()
            );

            result.add(dto);
        }

        return result;
    }
    private Vector2f tileCenter(int tileX, int tileY) {
        float widhtX = tileX * TILE_SIZE + TILE_SIZE * 0.5f;
        float widhtY = tileY * TILE_SIZE + TILE_SIZE * 0.5f;
        return new Vector2f(widhtX, widhtY);
    }

    private boolean isTileBlocked(int tileX, int tileY) {
        return occupiedTiles.containsKey(tileKey(tileX, tileY));
    }

    private String tileKey(int x, int y) {
        return x + "," + y;
    }

    public boolean placeBuilding(String clientId, String buildingType, int tileX, int tileY) {
        String key = tileKey(tileX, tileY);
        if (occupiedTiles.containsKey(key)) return false; 

        EntityId id = EntityId.random();

        // Tile center in world coords
        float wx = tileX * TILE_SIZE + TILE_SIZE * 0.5f;
        float wy = tileY * TILE_SIZE + TILE_SIZE * 0.5f;

        EntityState state = new EntityState(
                id,
                EntityType.BUILDING,
                clientId,
                new Vector2f(wx, wy),
                100f, 100f,
                0f, 0f
        );

        state.setBuildingType(buildingType);
        state.setTileX(tileX);
        state.setTileY(tileY);

        entities.put(id, state);
        occupiedTiles.put(key, id);
        return true;
    }

    private List<BuildingStateDto> buildBuildingStateList() {
        List<BuildingStateDto> result = new ArrayList<>();
        for (EntityState e : entities.values()) {
            if (e.getType() != EntityType.BUILDING) continue;

            BuildingStateDto dto = new BuildingStateDto(
                    e.getId().toString(),
                    e.getClientId(),
                    e.getBuildingType(),
                    e.getTileX() == null ? 0 : e.getTileX(),
                    e.getTileY() == null ? 0 : e.getTileY(),
                    e.getPosition()
            );
            result.add(dto);
        }
        return result;
    }


    /**
     * Erzeugt einen WorldSnapshotDto für den aktuellen Tick.
     * Dieser Snapshot kann dann über den ServerMessageBus an alle Clients gesendet werden.
     */
    public WorldSnapshotDto buildSnapshot() {
        WorldSnapshotDto snapshot = new WorldSnapshotDto();
        snapshot.setRoomId(roomId);
        snapshot.setTick(tick);
        snapshot.setPlayers(buildPlayerStateList());
        snapshot.setBuildings(buildBuildingStateList());
        return snapshot;
    }
}
