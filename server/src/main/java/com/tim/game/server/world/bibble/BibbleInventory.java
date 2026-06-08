package com.tim.game.server.world.bibble;

import com.tim.game.shared.bibble.BibbleInventoryDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Server-side ownership container. Bibbles themselves stay in BibbleManager by id.
 */
public class BibbleInventory {
    private final int maxActiveSlots;
    private final int maxStoredBibbles;
    private final List<String> activeSlots;
    private final List<String> terminalStorageIds = new ArrayList<>();
    private final List<String> workstationIds = new ArrayList<>();
    private final List<String> fencedAreaIds = new ArrayList<>();

    public BibbleInventory(int maxActiveSlots, int maxStoredBibbles) {
        this.maxActiveSlots = Math.max(0, Math.min(3, maxActiveSlots));
        this.maxStoredBibbles = Math.max(0, maxStoredBibbles);
        this.activeSlots = new ArrayList<>(Collections.nCopies(this.maxActiveSlots, null));
    }

    public boolean hasBibble(String bibbleId) {
        return activeSlots.contains(bibbleId)
                || terminalStorageIds.contains(bibbleId)
                || workstationIds.contains(bibbleId)
                || fencedAreaIds.contains(bibbleId);
    }

    public int firstFreeActiveSlot() {
        for (int i = 0; i < activeSlots.size(); i++) {
            if (activeSlots.get(i) == null) {
                return i;
            }
        }
        return -1;
    }

    public String getActiveAt(int slot) {
        if (slot < 0 || slot >= activeSlots.size()) {
            return null;
        }
        return activeSlots.get(slot);
    }

    public boolean activate(String bibbleId, int requestedSlot) {
        if (bibbleId == null || bibbleId.isBlank()) {
            return false;
        }
        int slot = requestedSlot >= 0 ? requestedSlot : firstFreeActiveSlot();
        if (slot < 0 || slot >= activeSlots.size()) {
            return false;
        }
        if (activeSlots.get(slot) != null) {
            return false;
        }
        removeEverywhere(bibbleId);
        activeSlots.set(slot, bibbleId);
        return true;
    }

    public boolean storeInTerminal(String bibbleId) {
        if (bibbleId == null || bibbleId.isBlank()) {
            return false;
        }
        if (!terminalStorageIds.contains(bibbleId) && terminalStorageIds.size() >= maxStoredBibbles) {
            return false;
        }
        removeEverywhere(bibbleId);
        if (!terminalStorageIds.contains(bibbleId)) {
            terminalStorageIds.add(bibbleId);
        }
        return true;
    }

    public boolean assignToWorkstation(String bibbleId) {
        if (bibbleId == null || bibbleId.isBlank()) {
            return false;
        }
        removeEverywhere(bibbleId);
        if (!workstationIds.contains(bibbleId)) {
            workstationIds.add(bibbleId);
        }
        return true;
    }

    public void removeEverywhere(String bibbleId) {
        for (int i = 0; i < activeSlots.size(); i++) {
            if (bibbleId != null && bibbleId.equals(activeSlots.get(i))) {
                activeSlots.set(i, null);
            }
        }
        terminalStorageIds.remove(bibbleId);
        workstationIds.remove(bibbleId);
        fencedAreaIds.remove(bibbleId);
    }

    public List<String> activeIds() {
        List<String> result = new ArrayList<>();
        for (String id : activeSlots) {
            if (id != null && !id.isBlank()) {
                result.add(id);
            }
        }
        return result;
    }

    public List<String> terminalIds() {
        return new ArrayList<>(terminalStorageIds);
    }

    public BibbleInventoryDto toDto() {
        BibbleInventoryDto dto = new BibbleInventoryDto();
        dto.setMaxActiveSlots(maxActiveSlots);
        dto.setActiveBibbleIds(new ArrayList<>(activeSlots));
        dto.setStoredBibbleIds(new ArrayList<>(terminalStorageIds));
        dto.setWorkstationBibbleIds(new ArrayList<>(workstationIds));
        dto.setFencedAreaBibbleIds(new ArrayList<>(fencedAreaIds));
        return dto;
    }
}
