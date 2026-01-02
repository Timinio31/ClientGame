package com.tim.game.shared.messaging;
/**Repräsentiert eine Nachricht vom Server zum Client.
 * Beispiel:
 *      „In Room 1 hat sich der Zustand geändert, hier ein WorldSnapshot“ oder „PlayerState-Update für Client 42“. 
 * Felder: 
 *      MessageType type → z. B. PLAYER_STATE, WORLD_SNAPSHOT, ENTITY_UPDATE 
 *      String roomId → zu welchem Room gehört das? 
 *      String targetClientId (nullable) → wenn null → Broadcast, sonst private Message 
 *      String payloadJson → JSON des z. B. PlayerStateDto oder WorldSnapshotDto 
 */

/**
 * Generische Event-Nachricht (Server -> Client).
 *
 * - type: z.B. PLAYER_STATE, WORLD_SNAPSHOT
 * - roomId: in welchem Raum ist das passiert
 * - targetClientId:
 *    - null = Broadcast an alle im Room
 *    - gesetzt = nur an diesen Client
 * - payloadJson: JSON-Repräsentation des DTOs (z.B. PlayerStateDto)
 */

public class EventMessage {

    private MessageType type;
    private String roomId;
    private String targetClientId; // optional, null = Broadcast
    private String payloadJson;

    public EventMessage() {
    }

    public EventMessage(MessageType type, String roomId, String targetClientId, String payloadJson) {
        this.type = type;
        this.roomId = roomId;
        this.targetClientId = targetClientId;
        this.payloadJson = payloadJson;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(String targetClientId) {
        this.targetClientId = targetClientId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    @Override
    public String toString() {
        return "EventMessage{" +
                "type=" + type +
                ", roomId='" + roomId + '\'' +
                ", targetClientId='" + targetClientId + '\'' +
                ", payloadJson='" + payloadJson + '\'' +
                '}';
    }
}