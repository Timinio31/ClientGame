package com.tim.game.server.world;

import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.model.EntityId;
import com.tim.game.shared.model.EntityType;
import com.tim.game.shared.model.Vector2f;

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

        EntityState state = new EntityState(
                id,
                EntityType.PLAYER,
                clientId,
                startPosition,
                100f,  // health
                100f,  // maxHealth
                100f,  // stamina
                100f   // maxStamina
        );

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
    public void movePlayer(String clientId, Vector2f direction, float speed) {
        EntityState player = getPlayerEntity(clientId);
        if (player == null) {
            return;
        }

        Vector2f pos = player.getPosition();
        if (pos == null) {
            pos = new Vector2f(0f, 0f);
        }

        // einfache Bewegung: pos += dir * speed
        pos.set(
                pos.getX() + direction.getX() * speed,
                pos.getY() + direction.getY() * speed
        );

        player.setPosition(pos);
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
                    state.getId(),
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

    /**
     * Erzeugt einen WorldSnapshotDto für den aktuellen Tick.
     * Dieser Snapshot kann dann über den ServerMessageBus an alle Clients gesendet werden.
     */
    public WorldSnapshotDto buildSnapshot() {
        WorldSnapshotDto snapshot = new WorldSnapshotDto();
        snapshot.setRoomId(roomId);
        snapshot.setTick(tick);
        snapshot.setPlayers(buildPlayerStateList());
        return snapshot;
    }
}
