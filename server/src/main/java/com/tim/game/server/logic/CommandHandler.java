package com.tim.game.server.logic;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.game.server.net.ServerMessageBus;
import com.tim.game.server.world.WorldState;
import com.tim.game.shared.DTOs.input.ActionInputDto;
import com.tim.game.shared.DTOs.input.BuildInputDto;
import com.tim.game.shared.DTOs.input.MoveInputDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.MessageType;
import com.tim.game.shared.model.Vector2f;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verarbeitet eingehende Commands und wendet sie auf den WorldState an.
 * Zusätzlich wird hier sichergestellt, dass jeder Client einmalig ein MAP_INIT erhält.
 */
public class CommandHandler {

    private final WorldState worldState;
    private final ServerMessageBus messageBus;
    private final ObjectMapper objectMapper;

    private final Set<String> initializedClientKeys = new HashSet<>();

    public CommandHandler(WorldState worldState, ServerMessageBus messageBus) {
        this.worldState = worldState;
        this.messageBus = messageBus;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Fallback-Konstruktor, falls ältere Tests/Startpunkte den Bus noch nicht übergeben.
     * Ohne MessageBus kann kein MAP_INIT verschickt werden.
     */
    public CommandHandler(WorldState worldState) {
        this(worldState, null);
    }

    public void applyCommands(List<CommandMessage> commands) {
        for (CommandMessage command : commands) {
            try {
                ensureClientInitialized(command);
                handleCommand(command);
            } catch (Exception e) {
                System.err.println("Error while handling command: " + command + " -> " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void ensureClientInitialized(CommandMessage commandMessage) {
        if (messageBus == null || commandMessage == null) {
            return;
        }

        String clientId = commandMessage.getClientId();
        if (clientId == null || clientId.isBlank()) {
            return;
        }

        String roomId = commandMessage.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            roomId = worldState.getRoomId();
        }

        String key = initKey(roomId, clientId);
        if (initializedClientKeys.contains(key)) {
            return;
        }

        MapInitDto mapInit = worldState.buildMapInit();
        messageBus.sendMapInitToClient(roomId, clientId, mapInit);
        initializedClientKeys.add(key);

        System.out.println("[Server] MAP_INIT sent to client=" + clientId + " room=" + roomId);
    }

    private void handleCommand(CommandMessage commandMessage) throws IOException {
        MessageType type = commandMessage.getType();

        if (type == null) {
            System.err.println("Received CommandMessage without type: " + commandMessage);
            return;
        }

        switch (type) {
            case MOVE -> handleMove(commandMessage);
            case ACTION -> handleAction(commandMessage);
            case BUILD -> handleBuild(commandMessage);
            default -> System.out.println("Unhandled command type: " + type + " commandMessage=" + commandMessage);
        }
    }

    private void handleMove(CommandMessage commandMessage) throws IOException {
        String clientId = commandMessage.getClientId();
        MoveInputDto input = objectMapper.readValue(commandMessage.getPayloadJson(), MoveInputDto.class);
        Vector2f direction = input.getDirection();

        if (direction == null) {
            return;
        }

        int directionX = (int) Math.signum(direction.getX());
        int directionY = (int) Math.signum(direction.getY());

        worldState.movePlayerStep(clientId, directionX, directionY);
    }

    private void handleAction(CommandMessage commandMessage) throws IOException {
        ActionInputDto input = objectMapper.readValue(commandMessage.getPayloadJson(), ActionInputDto.class);

        // Später echte Action-Logik:
        // - ATTACK -> Schaden berechnen
        // - INTERACT -> Kiste öffnen, Schalter umlegen etc.
        System.out.println("Received ACTION from " + commandMessage.getClientId() + ": " + input);
    }

    private void handleBuild(CommandMessage commandMessage) throws IOException {
        BuildInputDto input = objectMapper.readValue(commandMessage.getPayloadJson(), BuildInputDto.class);

        boolean ok = worldState.placeBuilding(
                commandMessage.getClientId(),
                input.getBuildingType(),
                input.getTileX(),
                input.getTileY()
        );

        if (!ok) {
            System.out.println("[Server] BUILD rejected by " + commandMessage.getClientId() + ": " + input);
        } else {
            System.out.println("[Server] BUILD placed by " + commandMessage.getClientId() + ": " + input);
        }
    }

    private String initKey(String roomId, String clientId) {
        return roomId + "|" + clientId;
    }
}
