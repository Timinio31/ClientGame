package com.tim.game.shared.bibble;

/**
 * Player-issued command for active Bibbles. Server validation decides whether it can be applied.
 */
public enum BibbleCommandType {
    FOLLOW,
    STAY,
    PATROL,
    WORK,
    ATTACK,
    DEFEND,
    RETURN_TO_TERMINAL
}
