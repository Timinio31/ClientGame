package com.tim.game.shared.messaging;
/** 
 * Generische Command-Nachricht (Client -> Server).
 *
 * Enthält:
 * - Typ (z.B. MOVE, BUILD)
 * - roomId (für Routing/World)
 * - clientId (welcher Spieler)
 * - payloadJson (JSON-Repräsentation des eigentlichen DTOs, z.B. MoveInputDto)
 */

public class CommandMessage{
    private MessageType type;
    private String roomId;
    private String clientId;

    // JSON-String des eigentlichen Payloads (z.B. MoveInputDto)
    private String payloadJson;

    public CommandMessage(){}

    public CommandMessage(MessageType type,String roomId, String clientId, String payloadJson){
        this.type = type;
        this.roomId = roomId;
        this.clientId = clientId;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    @Override
    public String toString() {
        return "CommandMessage{" +
                "type=" + type +
                ", roomId='" + roomId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", payloadJson='" + payloadJson + '\'' +
                '}';
    }

}