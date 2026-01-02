package com.tim.game.server.world;

import com.tim.game.shared.model.EntityId;
import com.tim.game.shared.model.EntityType;
import com.tim.game.shared.model.Vector2f;

/**
 * Ähnlich wie PlayerStateDto, wird aber nicht verschickt
 * kann intere infos tragen
 * 
 * 
 * WorldState speichert EntityState-Obkecte und baut daraus bei Bedarf DTOs.
 */

/**
 * Interner Server-Zustand einer Entity (Spieler, Monster, Gebäude, etc.).
 * Wird nicht direkt an Clients gesendet – dafür gibt es die DTOs.
 */
public class EntityState {

    private final EntityId id;
    private final EntityType type;

    private String clientId;    // nur bei PLAYER relevant, sonst null

    private Vector2f position;

    private float health;
    private float maxHealth;

    private float stamina;
    private float maxStamina;

    public EntityState(EntityId id,
                       EntityType type,
                       String clientId,
                       Vector2f position,
                       float health,
                       float maxHealth,
                       float stamina,
                       float maxStamina) {
        this.id = id;
        this.type = type;
        this.clientId = clientId;
        this.position = position;
        this.health = health;
        this.maxHealth = maxHealth;
        this.stamina = stamina;
        this.maxStamina = maxStamina;
    }

    public EntityId getId() {
        return id;
    }

    public EntityType getType() {
        return type;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Vector2f getPosition() {
        return position;
    }

    public void setPosition(Vector2f position) {
        this.position = position;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }

    public float getStamina() {
        return stamina;
    }

    public void setStamina(float stamina) {
        this.stamina = stamina;
    }

    public float getMaxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(float maxStamina) {
        this.maxStamina = maxStamina;
    }
}