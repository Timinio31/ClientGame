package com.tim.game.shared.DTOs.input;

/**
 * Client request for Bibble actions. The server validates ownership, settings, team size and target state.
 */
public class BibbleInputDto {
    private String action;
    private String bibbleId;
    private int teamSlot = -1;
    private String command;
    private String targetEntityId;
    private String targetPlayerId;

    public BibbleInputDto() {
    }

    public BibbleInputDto(String action, String bibbleId, int teamSlot, String command, String targetEntityId, String targetPlayerId) {
        this.action = action;
        this.bibbleId = bibbleId;
        this.teamSlot = teamSlot;
        this.command = command;
        this.targetEntityId = targetEntityId;
        this.targetPlayerId = targetPlayerId;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getBibbleId() { return bibbleId; }
    public void setBibbleId(String bibbleId) { this.bibbleId = bibbleId; }
    public int getTeamSlot() { return teamSlot; }
    public void setTeamSlot(int teamSlot) { this.teamSlot = teamSlot; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(String targetEntityId) { this.targetEntityId = targetEntityId; }
    public String getTargetPlayerId() { return targetPlayerId; }
    public void setTargetPlayerId(String targetPlayerId) { this.targetPlayerId = targetPlayerId; }

    @Override
    public String toString() {
        return "BibbleInputDto{" +
                "action='" + action + '\'' +
                ", bibbleId='" + bibbleId + '\'' +
                ", teamSlot=" + teamSlot +
                ", command='" + command + '\'' +
                ", targetEntityId='" + targetEntityId + '\'' +
                ", targetPlayerId='" + targetPlayerId + '\'' +
                '}';
    }
}
