package com.tim.game.shared.bibble;

import com.tim.game.shared.model.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Network-safe Bibble state. It is used for wild, owned, active, stored and assigned Bibbles.
 */
public class BibbleStateDto {
    private String bibbleId;
    private String ownerClientId;
    private String customName;
    private String speciesId;
    private String description;
    private BibbleType primaryType = BibbleType.NORMAL;
    private BibbleType secondaryType;
    private BibblePersonality personality = BibblePersonality.WILD;
    private BibbleLifecycleState lifecycleState = BibbleLifecycleState.WILD;
    private BibbleCommandType currentCommand = BibbleCommandType.FOLLOW;
    private BibbleStats stats = new BibbleStats();
    private List<BibbleAttack> attacks = new ArrayList<>();
    private Vector2f position;
    private int tileX;
    private int tileY;
    private int teamSlot = -1;
    private String workstationEntityId;
    private String fencedAreaId;
    private boolean frozen;

    /** -100 fear, 0 neutral, +100 love. */
    private int fearLove = 0;

    /** Derived 0..100 from fear/love intensity. High values mean stronger control/bond, but not always healthy. */
    private int connection = 0;

    /** Prepared capture value for later capture chance calculations. */
    private float captureScore = 0f;

    private int level = 1;
    private float experience = 0f;

    public BibbleStateDto() {
    }

    public BibbleStateDto copy() {
        BibbleStateDto copy = new BibbleStateDto();
        copy.bibbleId = bibbleId;
        copy.ownerClientId = ownerClientId;
        copy.customName = customName;
        copy.speciesId = speciesId;
        copy.description = description;
        copy.primaryType = primaryType;
        copy.secondaryType = secondaryType;
        copy.personality = personality;
        copy.lifecycleState = lifecycleState;
        copy.currentCommand = currentCommand;
        copy.stats = stats == null ? new BibbleStats() : stats.copy();
        copy.attacks = attacks == null ? new ArrayList<>() : new ArrayList<>(attacks);
        copy.position = position == null ? null : new Vector2f(position.getX(), position.getY());
        copy.tileX = tileX;
        copy.tileY = tileY;
        copy.teamSlot = teamSlot;
        copy.workstationEntityId = workstationEntityId;
        copy.fencedAreaId = fencedAreaId;
        copy.frozen = frozen;
        copy.fearLove = fearLove;
        copy.connection = connection;
        copy.captureScore = captureScore;
        copy.level = level;
        copy.experience = experience;
        return copy;
    }

    public void recomputeConnectionFromFearLove() {
        fearLove = clamp(fearLove, -100, 100);
        connection = Math.abs(fearLove);
        if (personality == BibblePersonality.LOYAL || personality == BibblePersonality.PROTECTIVE) {
            connection = clamp(connection + 8, 0, 100);
        }
        if (personality == BibblePersonality.REBELLIOUS || personality == BibblePersonality.WILD) {
            connection = clamp(connection - 8, 0, 100);
        }
    }

    public boolean isOwnedBy(String clientId) {
        return clientId != null && clientId.equals(ownerClientId);
    }

    public boolean isActiveTeamMember() {
        return lifecycleState == BibbleLifecycleState.OWNED_ACTIVE_FOLLOWING
                || lifecycleState == BibbleLifecycleState.OWNED_ACTIVE_STAYING
                || lifecycleState == BibbleLifecycleState.OWNED_ACTIVE_PATROLLING;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public String getBibbleId() { return bibbleId; }
    public void setBibbleId(String bibbleId) { this.bibbleId = bibbleId; }
    public String getOwnerClientId() { return ownerClientId; }
    public void setOwnerClientId(String ownerClientId) { this.ownerClientId = ownerClientId; }
    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }
    public String getSpeciesId() { return speciesId; }
    public void setSpeciesId(String speciesId) { this.speciesId = speciesId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BibbleType getPrimaryType() { return primaryType; }
    public void setPrimaryType(BibbleType primaryType) { this.primaryType = primaryType == null ? BibbleType.NORMAL : primaryType; }
    public BibbleType getSecondaryType() { return secondaryType; }
    public void setSecondaryType(BibbleType secondaryType) { this.secondaryType = secondaryType; }
    public BibblePersonality getPersonality() { return personality; }
    public void setPersonality(BibblePersonality personality) { this.personality = personality == null ? BibblePersonality.WILD : personality; }
    public BibbleLifecycleState getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(BibbleLifecycleState lifecycleState) { this.lifecycleState = lifecycleState == null ? BibbleLifecycleState.WILD : lifecycleState; }
    public BibbleCommandType getCurrentCommand() { return currentCommand; }
    public void setCurrentCommand(BibbleCommandType currentCommand) { this.currentCommand = currentCommand == null ? BibbleCommandType.FOLLOW : currentCommand; }
    public BibbleStats getStats() { return stats; }
    public void setStats(BibbleStats stats) { this.stats = stats == null ? new BibbleStats() : stats; }
    public List<BibbleAttack> getAttacks() { return attacks; }
    public void setAttacks(List<BibbleAttack> attacks) { this.attacks = attacks == null ? new ArrayList<>() : attacks; }
    public Vector2f getPosition() { return position; }
    public void setPosition(Vector2f position) { this.position = position; }
    public int getTileX() { return tileX; }
    public void setTileX(int tileX) { this.tileX = tileX; }
    public int getTileY() { return tileY; }
    public void setTileY(int tileY) { this.tileY = tileY; }
    public int getTeamSlot() { return teamSlot; }
    public void setTeamSlot(int teamSlot) { this.teamSlot = teamSlot; }
    public String getWorkstationEntityId() { return workstationEntityId; }
    public void setWorkstationEntityId(String workstationEntityId) { this.workstationEntityId = workstationEntityId; }
    public String getFencedAreaId() { return fencedAreaId; }
    public void setFencedAreaId(String fencedAreaId) { this.fencedAreaId = fencedAreaId; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public int getFearLove() { return fearLove; }
    public void setFearLove(int fearLove) { this.fearLove = clamp(fearLove, -100, 100); recomputeConnectionFromFearLove(); }
    public int getConnection() { return connection; }
    public void setConnection(int connection) { this.connection = clamp(connection, 0, 100); }
    public float getCaptureScore() { return captureScore; }
    public void setCaptureScore(float captureScore) { this.captureScore = captureScore; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public float getExperience() { return experience; }
    public void setExperience(float experience) { this.experience = Math.max(0f, experience); }
}
