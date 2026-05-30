package com.tim.game.shared.messaging;

/**
 * Typ einer Nachricht im Spielprotokoll.
 * Wird für Commands (Client->Server) und Events (Server->Client) genutzt.
 */
public enum MessageType {

    // Client → Server (Inputs)
    MOVE,
    ACTION,
    BUILD,
    INVENTORY,
    MINE,
    CAPTURE,
    SUMMON,
    ATTACK,
    //..

    // Server → Client (Updates)
    MAP_INIT,
    PLAYER_STATE,
    WORLD_SNAPSHOT,
    ENTITY_UPDATE,
    //..

    // Debug / Sonstiges
    PING,
    PONG
}
