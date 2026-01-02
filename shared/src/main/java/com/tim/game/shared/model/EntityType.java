package com.tim.game.shared.model;

/**
 * Enum zum beschreiben der Entity die wir haben
 *  Spieler
 *  Monster
 *  Gebäude
 *  Ressourcen-Nodes
 *  Projektile
 */

/**
 * genutzt in Worldstate -> welche logik für welches entity
 * genutzt in rendering -> welches sprite für welches entity
 */

public enum EntityType {
    PLAYER,
    MONSTER,
    BUILDING,
    RESOURCE_NODE,
    PROJECTILE
}