package com.tim.game.shared.DTOs.update;
import com.tim.game.shared.model.EntityId;
import com.tim.game.shared.model.EntityType;
import com.tim.game.shared.model.Vector2f;

/**
 * repräsentiert den zustand eines spielers, den der server an CLients schickt
 * bsp: Entity-id, position , zu welchen client er gehört...
 */

/**
 * DTO für den Zustand eines einzelnen Spielers.
 * Wird vom Server an die Clients gesendet (z.B. im WorldSnapshot oder bei Einzel-Updates).
 */
public class PlayerStateDto {
    private EntityId entityId;
    private String clientId;
    private EntityType type;
    private Vector2f position;

    private float health;
    private float maxHealth;

    private float stamina;
    private float maxStamina;

    public PlayerStateDto(){}

    public PlayerStateDto(EntityId entityId,String clientId,EntityType type,Vector2f position,float health,float maxHealth,float stamina,float maxStamina) {
        this.entityId = entityId;
        this.clientId = clientId;
        this.type = type;
        this.position = position;
        this.health = health;
        this.maxHealth = maxHealth;
        this.stamina = stamina;
        this.maxStamina = maxStamina;
    }

     public EntityId getEntityId() {
        return entityId;
    }

    public void setEntityId(EntityId entityId) {
        this.entityId = entityId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "PlayerStateDto{" +
                "entityId=" + entityId +
                ", clientId='" + clientId + '\'' +
                ", type=" + type +
                ", position=" + position +
                ", health=" + health +
                ", maxHealth=" + maxHealth +
                ", stamina=" + stamina +
                ", maxStamina=" + maxStamina +
                '}';
    }
}