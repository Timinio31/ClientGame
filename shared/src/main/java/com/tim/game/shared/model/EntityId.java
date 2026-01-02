package com.tim.game.shared.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Wrapper klasse fpr entity ids
 * Gibt zurück:
 *      klaren typ(entityid)
 *      bessere lesbarkeit
 *      weniger fehler bei copy-paste von Strings
 */

/**
 * Wrapper für eine eindeutige Entity-ID.
 * Statt überall Strings zu verwenden, nutzen wir diesen Typ für mehr Typsicherheit.
 */
public class EntityId {

    private final String id;

    public EntityId(String id) {
        this.id = id;
    }

    /**
     * Erzeugt eine zufällige eindeutige EntityId.
     */
    public static EntityId random() {
        return new EntityId(UUID.randomUUID().toString());
    }

    /**
     * Erzeugt eine EntityId aus einem bereits vorhandenen String.
     */
    public static EntityId of(String id) {
        return new EntityId(id);
    }

    /**
     * Convenience für z.B. Player-IDs, wenn du sie zusammensetzen willst:
     * "player:<clientId>"
     */
    public static EntityId player(String clientId) {
        return new EntityId("player:" + clientId);
    }

    public String getValue() {
        return id;
    }

    @Override
    public String toString() {
        return "EntityId{" +
                "id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityId)) return false;
        EntityId entityId = (EntityId) o;
        return Objects.equals(id, entityId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}