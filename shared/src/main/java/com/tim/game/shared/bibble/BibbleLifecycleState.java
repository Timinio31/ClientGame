package com.tim.game.shared.bibble;

/**
 * A Bibble must have exactly one lifecycle state at a time.
 */
public enum BibbleLifecycleState {
    WILD,
    OWNED_ACTIVE_FOLLOWING,
    OWNED_ACTIVE_STAYING,
    OWNED_ACTIVE_PATROLLING,
    OWNED_STORED_TERMINAL,
    OWNED_AT_WORKSTATION,
    OWNED_IN_FENCED_AREA,
    TRADE_PENDING,
    INCAPACITATED,
    DEAD
}
