package com.tim.game.shared.DTOs.update;

import com.tim.game.shared.bibble.BibbleStateDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamischer Snapshot der Welt für einen bestimmten Tick.
 * Die statische Map ist bewusst nicht mehr enthalten.
 * Sie wird einmalig über MessageType.MAP_INIT übertragen.
 */
public class WorldSnapshotDto {

    private String roomId;
    private long tick;

    private List<PlayerStateDto> players = new ArrayList<>();
    private List<BuildingStateDto> buildings = new ArrayList<>();
    private List<WorldItemStateDto> worldItems = new ArrayList<>();
    private List<BibbleStateDto> bibbles = new ArrayList<>();

    public WorldSnapshotDto() {
    }

    public WorldSnapshotDto(String roomId, long tick, List<PlayerStateDto> players, List<BuildingStateDto> buildings) {
        this.roomId = roomId;
        this.tick = tick;
        setPlayers(players);
        setBuildings(buildings);
    }

    public WorldSnapshotDto(String roomId,
                            long tick,
                            List<PlayerStateDto> players,
                            List<BuildingStateDto> buildings,
                            List<WorldItemStateDto> worldItems) {
        this(roomId, tick, players, buildings);
        setWorldItems(worldItems);
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public List<PlayerStateDto> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerStateDto> players) {
        this.players = players == null ? new ArrayList<>() : players;
    }

    public void addPlayer(PlayerStateDto playerState) {
        if (playerState != null) {
            players.add(playerState);
        }
    }

    public List<BuildingStateDto> getBuildings() {
        return buildings;
    }

    public void setBuildings(List<BuildingStateDto> buildings) {
        this.buildings = buildings == null ? new ArrayList<>() : buildings;
    }

    public void addBuilding(BuildingStateDto building) {
        if (building != null) {
            buildings.add(building);
        }
    }


    public List<WorldItemStateDto> getWorldItems() {
        return worldItems;
    }

    public void setWorldItems(List<WorldItemStateDto> worldItems) {
        this.worldItems = worldItems == null ? new ArrayList<>() : worldItems;
    }

    public void addWorldItem(WorldItemStateDto worldItem) {
        if (worldItem != null) {
            worldItems.add(worldItem);
        }
    }

    public List<BibbleStateDto> getBibbles() {
        return bibbles;
    }

    public void setBibbles(List<BibbleStateDto> bibbles) {
        this.bibbles = bibbles == null ? new ArrayList<>() : bibbles;
    }

    public void addBibble(BibbleStateDto bibble) {
        if (bibble != null) {
            bibbles.add(bibble);
        }
    }

    @Override
    public String toString() {
        return "WorldSnapshotDto{" +
                "roomId='" + roomId + '\'' +
                ", tick=" + tick +
                ", players=" + players.size() +
                ", buildings=" + buildings.size() +
                ", worldItems=" + worldItems.size() +
                ", bibbles=" + bibbles.size() +
                '}';
    }
}
