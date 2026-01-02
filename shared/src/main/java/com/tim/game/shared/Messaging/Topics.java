package com.tim.game.shared.messaging;

/**
 * Zentrale Definition aller Exchange-Namen und Routing-Key-Helfer.
 * Single Source of Truth für Client UND Server.
 */
public final class Topics {

    // Exchanges
    public static final String EXCHANGE_INPUTS = "game.inputs";
    public static final String EXCHANGE_UPDATES = "game.updates";

    private Topics() {
        // Utility-Klasse, soll nicht instanziiert werden
    }

    /**
     * Routing-Key für Client-Inputs.
     * Beispiel: room.1.client.42.input
     */
    public static String clientInput(String roomId, String clientId) {
        return "room." + roomId + ".client." + clientId + ".input";
    }

    /**
     * Routing-Key für Broadcast-Updates an alle Clients in einem Raum.
     * Beispiel: room.1.broadcast.state
     */
    public static String roomBroadcast(String roomId) {
        return "room." + roomId + ".broadcast.state";
    }

    /**
     * Routing-Key für private Updates an einen bestimmten Client.
     * Beispiel: room.1.client.42.private
     */
    public static String clientPrivate(String roomId, String clientId) {
        return "room." + roomId + ".client." + clientId + ".private";
    }
}
