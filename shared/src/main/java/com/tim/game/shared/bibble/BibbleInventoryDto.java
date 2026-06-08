package com.tim.game.shared.bibble;

import java.util.ArrayList;
import java.util.List;

/**
 * Player-facing Bibble inventory snapshot. Active slots are separate from terminal/storage ids.
 */
public class BibbleInventoryDto {
    private int maxActiveSlots = 3;
    private List<String> activeBibbleIds = new ArrayList<>();
    private List<String> storedBibbleIds = new ArrayList<>();
    private List<String> workstationBibbleIds = new ArrayList<>();
    private List<String> fencedAreaBibbleIds = new ArrayList<>();

    public BibbleInventoryDto() {
    }

    public int getMaxActiveSlots() { return maxActiveSlots; }
    public void setMaxActiveSlots(int maxActiveSlots) { this.maxActiveSlots = Math.max(0, maxActiveSlots); }
    public List<String> getActiveBibbleIds() { return activeBibbleIds; }
    public void setActiveBibbleIds(List<String> activeBibbleIds) { this.activeBibbleIds = activeBibbleIds == null ? new ArrayList<>() : activeBibbleIds; }
    public List<String> getStoredBibbleIds() { return storedBibbleIds; }
    public void setStoredBibbleIds(List<String> storedBibbleIds) { this.storedBibbleIds = storedBibbleIds == null ? new ArrayList<>() : storedBibbleIds; }
    public List<String> getWorkstationBibbleIds() { return workstationBibbleIds; }
    public void setWorkstationBibbleIds(List<String> workstationBibbleIds) { this.workstationBibbleIds = workstationBibbleIds == null ? new ArrayList<>() : workstationBibbleIds; }
    public List<String> getFencedAreaBibbleIds() { return fencedAreaBibbleIds; }
    public void setFencedAreaBibbleIds(List<String> fencedAreaBibbleIds) { this.fencedAreaBibbleIds = fencedAreaBibbleIds == null ? new ArrayList<>() : fencedAreaBibbleIds; }
}
