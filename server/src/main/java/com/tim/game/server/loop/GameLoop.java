package com.tim.game.server.loop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.game.server.logic.CommandHandler;
import com.tim.game.server.net.ServerMessageBus;
import com.tim.game.server.world.WorldState;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.MessageType;

import java.util.List;

/**
 * Einfacher GameLoop mit fester Tickrate.
 * Pro Tick:
 * - Commands aus der Queue holen
 * - auf den WorldState anwenden
 * - Tick erhöhen
 * - dynamischen WorldSnapshot bauen und an alle Clients senden
 */
public class GameLoop implements Runnable {

    private final WorldState worldState;
    private final ServerMessageBus messageBus;
    private final CommandHandler commandHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int ticksPerSecond;
    private volatile boolean running;

    public GameLoop(WorldState worldState,
                    ServerMessageBus messageBus,
                    CommandHandler commandHandler,
                    int ticksPerSecond) {
        this.worldState = worldState;
        this.messageBus = messageBus;
        this.commandHandler = commandHandler;
        this.ticksPerSecond = ticksPerSecond;
        this.running = false;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread loopThread = new Thread(this, "GameLoop-" + worldState.getRoomId());
        loopThread.start();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        final long tickDurationMillis = 1000L / ticksPerSecond;

        while (running) {
            long startTime = System.currentTimeMillis();

            try {
                step();
            } catch (Exception e) {
                System.err.println("Error in game loop: " + e.getMessage());
                e.printStackTrace();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = tickDurationMillis - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void step() throws Exception {
        List<CommandMessage> commands = messageBus.pollCommands();
        commandHandler.applyCommands(commands);

        worldState.incrementTick();

        WorldSnapshotDto snapshot = worldState.buildDynamicSnapshot(worldState.getTick());
        DebugConfig.log(DebugCategory.SNAPSHOT,
                "Snapshot tick=" + snapshot.getTick()
                        + " players=" + snapshot.getPlayers().size()
                        + " buildings=" + snapshot.getBuildings().size());

        String payloadJson = objectMapper.writeValueAsString(snapshot);

        EventMessage event = new EventMessage(
                MessageType.WORLD_SNAPSHOT,
                worldState.getRoomId(),
                null,
                payloadJson
        );

        messageBus.broadcastRoomState(event);
    }
}
