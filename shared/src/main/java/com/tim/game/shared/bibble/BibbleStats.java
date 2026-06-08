package com.tim.game.shared.bibble;

/**
 * Numeric Bibble stats. Values are deliberately plain DTO fields for JSON transfer.
 */
public class BibbleStats {
    private float health = 50f;
    private float maxHealth = 50f;
    private float physicalAttack = 8f;
    private float psychicAttack = 4f;
    private float physicalDefense = 5f;
    private float psychicDefense = 5f;
    private float speed = 5f;
    private float workSpeed = 1f;
    private float craftingSkill = 1f;
    private float carryCapacity = 5f;
    private float obedience = 50f;
    private float rebellion = 10f;

    public BibbleStats() {
    }

    public static BibbleStats baseFor(BibbleType type, float multiplier) {
        float m = multiplier <= 0f ? 1f : multiplier;
        BibbleStats stats = new BibbleStats();
        BibbleType normalized = type == null ? BibbleType.NORMAL : type;
        switch (normalized) {
            case FIRE -> {
                stats.physicalAttack = 10f;
                stats.psychicAttack = 7f;
                stats.workSpeed = 1.15f;
            }
            case WATER -> {
                stats.maxHealth = 58f;
                stats.health = 58f;
                stats.psychicDefense = 8f;
                stats.craftingSkill = 1.10f;
            }
            case NATURE -> {
                stats.carryCapacity = 7f;
                stats.craftingSkill = 1.20f;
                stats.rebellion = 6f;
            }
            case STONE -> {
                stats.maxHealth = 70f;
                stats.health = 70f;
                stats.physicalDefense = 10f;
                stats.speed = 3.5f;
            }
            case METAL -> {
                stats.physicalDefense = 12f;
                stats.workSpeed = 1.20f;
                stats.speed = 4f;
            }
            case ELECTRIC -> {
                stats.speed = 8f;
                stats.psychicAttack = 8f;
                stats.workSpeed = 1.25f;
            }
            case PSYCHIC -> {
                stats.psychicAttack = 12f;
                stats.psychicDefense = 10f;
                stats.physicalDefense = 3f;
            }
            case SHADOW -> {
                stats.speed = 7f;
                stats.physicalAttack = 9f;
                stats.rebellion = 20f;
            }
            case LIGHT -> {
                stats.psychicDefense = 12f;
                stats.obedience = 65f;
                stats.rebellion = 5f;
            }
            case MACHINE -> {
                stats.workSpeed = 1.45f;
                stats.craftingSkill = 1.30f;
                stats.carryCapacity = 8f;
            }
            case TOXIC -> {
                stats.physicalAttack = 7f;
                stats.psychicAttack = 9f;
                stats.rebellion = 16f;
            }
            case ICE -> {
                stats.psychicAttack = 8f;
                stats.psychicDefense = 8f;
                stats.speed = 4.5f;
            }
            default -> {
            }
        }
        stats.scaleBaseStats(m);
        return stats;
    }

    private void scaleBaseStats(float multiplier) {
        health *= multiplier;
        maxHealth *= multiplier;
        physicalAttack *= multiplier;
        psychicAttack *= multiplier;
        physicalDefense *= multiplier;
        psychicDefense *= multiplier;
        speed *= multiplier;
        workSpeed *= multiplier;
        craftingSkill *= multiplier;
        carryCapacity *= multiplier;
    }

    public BibbleStats copy() {
        BibbleStats copy = new BibbleStats();
        copy.health = health;
        copy.maxHealth = maxHealth;
        copy.physicalAttack = physicalAttack;
        copy.psychicAttack = psychicAttack;
        copy.physicalDefense = physicalDefense;
        copy.psychicDefense = psychicDefense;
        copy.speed = speed;
        copy.workSpeed = workSpeed;
        copy.craftingSkill = craftingSkill;
        copy.carryCapacity = carryCapacity;
        copy.obedience = obedience;
        copy.rebellion = rebellion;
        return copy;
    }

    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = health; }
    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
    public float getPhysicalAttack() { return physicalAttack; }
    public void setPhysicalAttack(float physicalAttack) { this.physicalAttack = physicalAttack; }
    public float getPsychicAttack() { return psychicAttack; }
    public void setPsychicAttack(float psychicAttack) { this.psychicAttack = psychicAttack; }
    public float getPhysicalDefense() { return physicalDefense; }
    public void setPhysicalDefense(float physicalDefense) { this.physicalDefense = physicalDefense; }
    public float getPsychicDefense() { return psychicDefense; }
    public void setPsychicDefense(float psychicDefense) { this.psychicDefense = psychicDefense; }
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
    public float getWorkSpeed() { return workSpeed; }
    public void setWorkSpeed(float workSpeed) { this.workSpeed = workSpeed; }
    public float getCraftingSkill() { return craftingSkill; }
    public void setCraftingSkill(float craftingSkill) { this.craftingSkill = craftingSkill; }
    public float getCarryCapacity() { return carryCapacity; }
    public void setCarryCapacity(float carryCapacity) { this.carryCapacity = carryCapacity; }
    public float getObedience() { return obedience; }
    public void setObedience(float obedience) { this.obedience = obedience; }
    public float getRebellion() { return rebellion; }
    public void setRebellion(float rebellion) { this.rebellion = rebellion; }
}
