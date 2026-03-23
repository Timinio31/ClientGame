package com.tim.game.server.logic;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.game.server.world.WorldState;
import com.tim.game.shared.DTOs.input.ActionInputDto;
import com.tim.game.shared.DTOs.input.BuildInputDto;
import com.tim.game.shared.DTOs.input.MoveInputDto;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.MessageType;
import com.tim.game.shared.model.Vector2f;

import java.io.IOException;
import java.util.List;

/**
 * Verarbeitet eingehende Commands und wendet sie auf den WorldState an.
 */
public class CommandHandler {

    private final WorldState worldState;
    private final ObjectMapper objectMapper;

    // einfache, feste Geschwindigkeit pro Tick (später konfigurierbar)
    private static final float MOVE_SPEED_PER_TICK = 0.1f;

    public CommandHandler(WorldState worldState) {
        this.worldState = worldState;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Nimmt alle Commands eines Ticks und verarbeitet sie.
     */
    public void applyCommands(List<CommandMessage> commands) {
        for (CommandMessage command : commands) {
            try {
                handleCommand(command);
            } catch (Exception e) {
                System.err.println("Error while handling command: " + command + " -> " + e.getMessage());
                e.printStackTrace();
            }
        }
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
            default -> {
                // andere Typen (PING/PONG etc.) erstmal nur loggen
                System.out.println("Unhandled command type: " + type + " commandMessage=" + commandMessage);
            }
        }
    }

    private void handleMove(CommandMessage commandMessage) throws IOException {
        String clientId = commandMessage.getClientId();

        MoveInputDto input = objectMapper.readValue(commandMessage.getPayloadJson(), MoveInputDto.class);
        Vector2f direction = input.getDirection();

        if (direction == null) return;

        int directionX = (int) Math.signum(direction.getX());
        int directionY = (int) Math.signum(direction.getY());

        worldState.movePlayerStep(clientId, directionX, directionY);
    }


    private void handleAction(CommandMessage commandMessage) throws IOException {
        ActionInputDto input = objectMapper.readValue(commandMessage.getPayloadJson(), ActionInputDto.class);

        // TODO: Hier später echte Action-Logik:
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
            System.out.println("[Server] BUILD rejected (occupied) by " + commandMessage.getClientId() + ": " + input);
        } else {
            System.out.println("[Server] BUILD placed by " + commandMessage.getClientId() + ": " + input);
        }
    }

}