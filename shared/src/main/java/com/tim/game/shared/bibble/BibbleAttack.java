package com.tim.game.shared.bibble;

/**
 * Attack definition assigned to a Bibble. Max four active attacks are enforced server-side later.
 */
public class BibbleAttack {
    private String attackId;
    private String displayName;
    private BibbleType type = BibbleType.NORMAL;
    private boolean psychic;
    private float power;
    private int cooldownTicks;

    public BibbleAttack() {
    }

    public BibbleAttack(String attackId, String displayName, BibbleType type, boolean psychic, float power, int cooldownTicks) {
        this.attackId = attackId;
        this.displayName = displayName;
        this.type = type == null ? BibbleType.NORMAL : type;
        this.psychic = psychic;
        this.power = power;
        this.cooldownTicks = cooldownTicks;
    }

    public String getAttackId() { return attackId; }
    public void setAttackId(String attackId) { this.attackId = attackId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public BibbleType getType() { return type; }
    public void setType(BibbleType type) { this.type = type == null ? BibbleType.NORMAL : type; }
    public boolean isPsychic() { return psychic; }
    public void setPsychic(boolean psychic) { this.psychic = psychic; }
    public float getPower() { return power; }
    public void setPower(float power) { this.power = power; }
    public int getCooldownTicks() { return cooldownTicks; }
    public void setCooldownTicks(int cooldownTicks) { this.cooldownTicks = cooldownTicks; }
}
