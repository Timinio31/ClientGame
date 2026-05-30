package com.tim.game.server;

import com.tim.game.server.logic.CommandHandler;
import com.tim.game.server.loop.GameLoop;
import com.tim.game.server.net.ServerMessageBus;
import com.tim.game.server.net.ServerRabbitConnection;
import com.tim.game.server.world.WorldState;
import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;

public class GameServerMain {

    public static void main(String[] args) {
        DebugConfig.setGlobalEnabled(true);
        DebugConfig.disableAll();
        DebugConfig.enable(DebugCategory.MAP);
        DebugConfig.enable(DebugCategory.MOVEMENT);
        DebugConfig.enable(DebugCategory.NETWORK);
        DebugConfig.enable(DebugCategory.BUILDING);

        ServerConfig config = ServerConfig.localDefault();
        ServerRabbitConnection rabbit = new ServerRabbitConnection(config);

        try {
            rabbit.connect();
            System.out.println("[Server] RabbitMQ connected. Room=" + config.getRoomId());

            ServerMessageBus bus = new ServerMessageBus(rabbit);
            bus.startConsumingCommands();
            System.out.println("[Server] Started consuming commands...");

            WorldState worldState = new WorldState(config.getRoomId());
            CommandHandler commandHandler = new CommandHandler(worldState, bus);

            GameLoop loop = new GameLoop(worldState, bus, commandHandler, 20);
            loop.start();

            System.out.println("[Server] GameLoop started (20 TPS). Press Ctrl+C to stop.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("[Server] Shutting down...");
                    loop.stop();
                    rabbit.close();
                    System.out.println("[Server] Shutdown complete.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            System.err.println("[Server] Failed to start: " + e.getMessage());
            e.printStackTrace();
            try {
                rabbit.close();
            } catch (Exception ignored) {
            }
            System.exit(1);
        }
    }
}
