package com.tim.game.server.logic;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.game.server.world.WorldState;
import com.tim.game.shared.DTOs.input.ActionInputDto;
import com.tim.game.shared.DTOs.input.BuildInputDto;
import com.tim.game.shared.DTOs.input.MoveInputDto;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.MessageType;

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
        for (CommandMessage cmd : commands) {
            try {
                handleCommand(cmd);
            } catch (Exception e) {
                System.err.println("Error while handling command: " + cmd + " -> " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(CommandMessage cmd) throws IOException {
        MessageType type = cmd.getType();

        if (type == null) {
            System.err.println("Received CommandMessage without type: " + cmd);
            return;
        }

        switch (type) {
            case MOVE -> handleMove(cmd);
            case ACTION -> handleAction(cmd);
            case BUILD -> handleBuild(cmd);
            default -> {
                // andere Typen (PING/PONG etc.) erstmal nur loggen
                System.out.println("Unhandled command type: " + type + " cmd=" + cmd);
            }
        }
    }

    private void handleMove(CommandMessage cmd) throws IOException {
        MoveInputDto input = objectMapper.readValue(cmd.getPayloadJson(), MoveInputDto.class);

        if (input.getDirection() == null) {
            return;
        }

        String clientId = cmd.getClientId();
        worldState.movePlayer(clientId, input.getDirection(), MOVE_SPEED_PER_TICK);
    }

    private void handleAction(CommandMessage cmd) throws IOException {
        ActionInputDto input = objectMapper.readValue(cmd.getPayloadJson(), ActionInputDto.class);

        // TODO: Hier später echte Action-Logik:
        // - ATTACK -> Schaden berechnen
        // - INTERACT -> Kiste öffnen, Schalter umlegen etc.
        System.out.println("Received ACTION from " + cmd.getClientId() + ": " + input);
    }

    private void handleBuild(CommandMessage cmd) throws IOException {
        BuildInputDto input = objectMapper.readValue(cmd.getPayloadJson(), BuildInputDto.class);

        // TODO: Später: worldState.buildStructure(...)
        System.out.println("Received BUILD from " + cmd.getClientId() + ": " + input);
    }
}