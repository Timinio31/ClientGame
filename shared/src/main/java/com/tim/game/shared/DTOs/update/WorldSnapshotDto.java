package com.tim.game.shared.DTOs.update;

import java.util.ArrayList;
import java.util.List;

/**
 * Speichert die ganze welt, mit allen entitys zu einem bestimmen zeitpunkt
 * beinhaltet roomId, tick, und Zustand aller spieler
 * 
 * 
 */
/**
 * Snapshot der Welt für einen bestimmten Tick.
 * Zunächst nur mit Spielerzuständen, später erweiterbar um Monster, Gebäude etc.
 */
public class WorldSnapshotDto {

    private String roomId;
    private long tick;

    private List<PlayerStateDto> players = new ArrayList<>();

    public WorldSnapshotDto() {
    }

    public WorldSnapshotDto(String roomId, long tick, List<PlayerStateDto> players) {
        this.roomId = roomId;
        this.tick = tick;
        if (players != null) {
            this.players = players;
        }
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
        this.players = players;
    }

    public void addPlayer(PlayerStateDto playerState) {
        this.players.add(playerState);
    }

    @Override
    public String toString() {
        return "WorldSnapshotDto{" +
                "roomId='" + roomId + '\'' +
                ", tick=" + tick +
                ", players=" + players +
                '}';
    }
}
