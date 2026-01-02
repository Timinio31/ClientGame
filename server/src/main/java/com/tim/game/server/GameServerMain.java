package com.tim.game.server;

import com.tim.game.server.logic.CommandHandler;
import com.tim.game.server.loop.GameLoop;
import com.tim.game.server.net.ServerMessageBus;
import com.tim.game.server.net.ServerRabbitConnection;
import com.tim.game.server.world.WorldState;
import com.tim.game.shared.model.Vector2f;

public class GameServerMain {

    public static void main(String[] args) {
        // Config laden 
        // Wenn du noch keine localDefault() hast: siehe Kommentar weiter unten.
        ServerConfig config = ServerConfig.localDefault();

        // RabbitMQ Connection + Topology
        ServerRabbitConnection rabbit = new ServerRabbitConnection(config);

        try {
            rabbit.connect(); // ConnectionFactory, Connection, Channel, Exchanges/Queue/Bindings
            System.out.println("[Server] RabbitMQ connected. Room=" + config.getRoomId());

            // MessageBus starten (Commands konsumieren)
            ServerMessageBus bus = new ServerMessageBus(rabbit);
            bus.startConsumingCommands();
            System.out.println("[Server] Started consuming commands...");

            // WorldState erstellen (Server ist Autorität)
            WorldState worldState = new WorldState(config.getRoomId());

            // Optional: Für Debug einen Spieler spawnen (später beim "JOIN"-Flow)
            // worldState.spawnPlayerForClient("debug-client", new Vector2f(0f, 0f));

            // CommandHandler (interpretiert Commands -> WorldState Updates)
            CommandHandler commandHandler = new CommandHandler(worldState);

            // GameLoop starten (20 Ticks/sec als Startwert)
            GameLoop loop = new GameLoop(worldState, bus, commandHandler, 20);
            loop.start();

            System.out.println("[Server] GameLoop started (20 TPS). Press Ctrl+C to stop.");

            // 7) Shutdown Hook (sauberer Shutdown)
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
