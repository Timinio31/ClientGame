package com.tim.game.server.world.bibble;

import com.tim.game.server.world.EntityState;
import com.tim.game.shared.DTOs.input.BibbleInputDto;
import com.tim.game.server.world.inventory.PlayerInventory;
import com.tim.game.shared.bibble.BibbleCatalog;
import com.tim.game.shared.bibble.BibbleCommandType;
import com.tim.game.shared.bibble.BibbleInventoryDto;
import com.tim.game.shared.bibble.BibbleLifecycleState;
import com.tim.game.shared.bibble.BibbleStateDto;
import com.tim.game.shared.config.WorldSettings;
import com.tim.game.shared.model.Vector2f;
import com.tim.game.shared.DTOs.update.ItemStackDto;
import com.tim.game.shared.inventory.ItemCatalog;
import com.tim.game.shared.workstation.WorkstationCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Server-authoritative Bibble ownership and first movement/command layer.
 * Capture, breeding, hacking, trading confirmation, fenced-area simulation and workstation recipes are explicit TODO extensions.
 */
public class BibbleManager {
    private final WorldSettings settings;
    private final long seed;
    private final Map<String, BibbleInventory> inventoriesByClient = new LinkedHashMap<>();
    private final Map<String, BibbleStateDto> bibblesById = new LinkedHashMap<>();

    public BibbleManager(WorldSettings settings, long seed) {
        this.settings = settings == null ? WorldSettings.defaults() : settings.normalizedCopy();
        this.seed = seed;
    }

    public void spawnWildBibblesAroundSpawns(List<Vector2f> spawnPoints) {
        if (!settings.isBibblesEnabled() || !settings.isBibbleSpawningEnabled()) {
            return;
        }
        if (spawnPoints == null || spawnPoints.isEmpty() || containsWildBibbles()) {
            return;
        }
        int count = Math.min(6, Math.max(2, spawnPoints.size() * 2));
        for (int i = 0; i < count; i++) {
            Vector2f spawn = spawnPoints.get(i % spawnPoints.size());
            BibbleStateDto wild = BibbleCatalog.createWildBibble(i, seed);
            int tileX = (int) Math.floor(spawn.getX()) + ((i % 3) - 1);
            int tileY = (int) Math.floor(spawn.getY()) + ((i / 3) - 1);
            moveBibbleTo(wild, tileX, tileY);
            bibblesById.put(wild.getBibbleId(), wild);
        }
    }

    private boolean containsWildBibbles() {
        for (BibbleStateDto bibble : bibblesById.values()) {
            if (bibble != null && bibble.getLifecycleState() == BibbleLifecycleState.WILD) {
                return true;
            }
        }
        return false;
    }

    public void ensurePlayerInitialized(String clientId, EntityState player) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }
        BibbleInventory inventory = inventoriesByClient.computeIfAbsent(clientId,
                ignored -> new BibbleInventory(settings.getMaxActiveBibbles(), settings.getMaxStoredBibbles()));
        if (!settings.isBibblesEnabled() || !settings.isStarterBibblesEnabled()) {
            return;
        }
        if (!inventory.activeIds().isEmpty() || !inventory.terminalIds().isEmpty()) {
            return;
        }

        BibbleStateDto starter = BibbleCatalog.createStarterBibble(clientId, 0, seed);
        setBibblePositionNearPlayer(starter, player, 0);
        bibblesById.put(starter.getBibbleId(), starter);
        if (settings.getMaxActiveBibbles() > 0) {
            inventory.activate(starter.getBibbleId(), 0);
            starter.setTeamSlot(0);
            starter.setLifecycleState(BibbleLifecycleState.OWNED_ACTIVE_FOLLOWING);
            starter.setCurrentCommand(BibbleCommandType.FOLLOW);
        } else {
            inventory.storeInTerminal(starter.getBibbleId());
        }
    }

    public boolean handleInput(String clientId, BibbleInputDto input, EntityState player, List<EntityState> buildings, PlayerInventory playerInventory) {
        if (!settings.isBibblesEnabled()) {
            return false;
        }
        if (clientId == null || input == null) {
            return false;
        }
        ensurePlayerInitialized(clientId, player);
        BibbleInventory inventory = inventoriesByClient.get(clientId);
        if (inventory == null) {
            return false;
        }
        String action = normalize(input.getAction());
        return switch (action) {
            case "TOGGLE_SLOT" -> toggleSlot(clientId, inventory, input.getTeamSlot(), player);
            case "RECALL_ALL" -> recallAll(clientId, inventory);
            case "FOLLOW_ALL" -> commandAll(clientId, BibbleCommandType.FOLLOW);
            case "STAY_ALL" -> commandAll(clientId, BibbleCommandType.STAY);
            case "PATROL_ALL" -> commandAll(clientId, BibbleCommandType.PATROL);
            case "ASSIGN_NEAREST_WORKSTATION" -> assignNearestWorkstation(clientId, inventory, input.getBibbleId(), player, buildings);
            case "CAPTURE_NEAREST" -> captureNearestWildBibble(clientId, inventory, player, playerInventory);
            default -> false;
        };
    }

    public void updateFollowing(Map<String, EntityState> playersByClient) {
        boolean frozen = !settings.isBibblesEnabled() && settings.isBibblesFrozenWhenDisabled();
        for (BibbleStateDto bibble : bibblesById.values()) {
            bibble.setFrozen(frozen);
            if (frozen || !settings.isBibblesEnabled() || !bibble.isActiveTeamMember()) {
                continue;
            }
            if (bibble.getLifecycleState() != BibbleLifecycleState.OWNED_ACTIVE_FOLLOWING) {
                continue;
            }
            EntityState owner = playersByClient.get(bibble.getOwnerClientId());
            if (owner == null) {
                continue;
            }
            int ownerX = owner.getTileX() == null ? 0 : owner.getTileX();
            int ownerY = owner.getTileY() == null ? 0 : owner.getTileY();
            int distance = Math.abs(ownerX - bibble.getTileX()) + Math.abs(ownerY - bibble.getTileY());
            if (distance >= settings.getBibbleFollowTeleportDistance()) {
                setBibblePositionAround(bibble, ownerX, ownerY, bibble.getTeamSlot());
            } else if (distance > 2) {
                int nextX = bibble.getTileX() + Integer.compare(ownerX, bibble.getTileX());
                int nextY = bibble.getTileY() + Integer.compare(ownerY, bibble.getTileY());
                moveBibbleTo(bibble, nextX, nextY);
            }
        }
    }

    public BibbleInventoryDto buildInventoryDto(String clientId) {
        BibbleInventory inventory = inventoriesByClient.get(clientId);
        if (inventory == null) {
            BibbleInventoryDto dto = new BibbleInventoryDto();
            dto.setMaxActiveSlots(settings.getMaxActiveBibbles());
            return dto;
        }
        return inventory.toDto();
    }

    public List<BibbleStateDto> buildSnapshotBibbles() {
        List<BibbleStateDto> result = new ArrayList<>();
        for (BibbleStateDto bibble : bibblesById.values()) {
            result.add(bibble.copy());
        }
        return result;
    }

    private boolean toggleSlot(String clientId, BibbleInventory inventory, int teamSlot, EntityState player) {
        int slot = Math.max(0, Math.min(settings.getMaxActiveBibbles() - 1, teamSlot));
        if (slot < 0) {
            return false;
        }
        String activeId = inventory.getActiveAt(slot);
        if (activeId != null) {
            BibbleStateDto active = bibblesById.get(activeId);
            if (active != null && active.isOwnedBy(clientId)) {
                inventory.storeInTerminal(activeId);
                active.setTeamSlot(-1);
                active.setLifecycleState(BibbleLifecycleState.OWNED_STORED_TERMINAL);
                active.setCurrentCommand(BibbleCommandType.RETURN_TO_TERMINAL);
                return true;
            }
        }

        for (String storedId : inventory.terminalIds()) {
            BibbleStateDto stored = bibblesById.get(storedId);
            if (stored == null || !stored.isOwnedBy(clientId)) {
                continue;
            }
            if (inventory.activate(storedId, slot)) {
                stored.setTeamSlot(slot);
                stored.setLifecycleState(BibbleLifecycleState.OWNED_ACTIVE_FOLLOWING);
                stored.setCurrentCommand(BibbleCommandType.FOLLOW);
                setBibblePositionNearPlayer(stored, player, slot);
                return true;
            }
        }
        return false;
    }

    private boolean recallAll(String clientId, BibbleInventory inventory) {
        boolean changed = false;
        for (String bibbleId : new ArrayList<>(inventory.activeIds())) {
            BibbleStateDto bibble = bibblesById.get(bibbleId);
            if (bibble != null && bibble.isOwnedBy(clientId)) {
                inventory.storeInTerminal(bibbleId);
                bibble.setTeamSlot(-1);
                bibble.setLifecycleState(BibbleLifecycleState.OWNED_STORED_TERMINAL);
                bibble.setCurrentCommand(BibbleCommandType.RETURN_TO_TERMINAL);
                changed = true;
            }
        }
        return changed;
    }

    private boolean commandAll(String clientId, BibbleCommandType command) {
        boolean changed = false;
        for (BibbleStateDto bibble : bibblesById.values()) {
            if (!bibble.isOwnedBy(clientId) || !bibble.isActiveTeamMember()) {
                continue;
            }
            bibble.setCurrentCommand(command);
            if (command == BibbleCommandType.FOLLOW) {
                bibble.setLifecycleState(BibbleLifecycleState.OWNED_ACTIVE_FOLLOWING);
            } else if (command == BibbleCommandType.STAY) {
                bibble.setLifecycleState(BibbleLifecycleState.OWNED_ACTIVE_STAYING);
            } else if (command == BibbleCommandType.PATROL) {
                bibble.setLifecycleState(BibbleLifecycleState.OWNED_ACTIVE_PATROLLING);
            }
            changed = true;
        }
        return changed;
    }

    private boolean assignNearestWorkstation(String clientId,
                                             BibbleInventory inventory,
                                             String requestedBibbleId,
                                             EntityState player,
                                             List<EntityState> buildings) {
        if (!settings.isBibbleWorkstationAutomationEnabled() || settings.getMaxBibblesPerWorkstation() <= 0) {
            return false;
        }
        String bibbleId = requestedBibbleId == null || requestedBibbleId.isBlank()
                ? firstActiveOwnedId(clientId, inventory)
                : requestedBibbleId;
        BibbleStateDto bibble = bibblesById.get(bibbleId);
        if (bibble == null || !bibble.isOwnedBy(clientId) || !bibble.isActiveTeamMember()) {
            return false;
        }
        EntityState workstation = findNearestWorkstation(player, buildings);
        if (workstation == null) {
            return false;
        }
        inventory.assignToWorkstation(bibbleId);
        bibble.setTeamSlot(-1);
        bibble.setWorkstationEntityId(workstation.getId().getValue());
        bibble.setLifecycleState(BibbleLifecycleState.OWNED_AT_WORKSTATION);
        bibble.setCurrentCommand(BibbleCommandType.WORK);
        if (workstation.getTileX() != null && workstation.getTileY() != null) {
            moveBibbleTo(bibble, workstation.getTileX(), workstation.getTileY());
        }
        return true;
    }

    public boolean attackBibbleAt(String clientId, EntityState player, int targetTileX, int targetTileY, int damage) {
        if (!settings.isBibbleCombatEnabled() || player == null) {
            return false;
        }
        BibbleStateDto target = null;
        int bestDistance = Integer.MAX_VALUE;
        for (BibbleStateDto bibble : bibblesById.values()) {
            if (bibble == null || bibble.getLifecycleState() == BibbleLifecycleState.DEAD || bibble.getLifecycleState() == BibbleLifecycleState.OWNED_STORED_TERMINAL) {
                continue;
            }
            if (!settings.isBibbleFriendlyFireEnabled() && bibble.isOwnedBy(clientId)) {
                continue;
            }
            int distanceToTarget = Math.abs(targetTileX - bibble.getTileX()) + Math.abs(targetTileY - bibble.getTileY());
            if (distanceToTarget <= 1 && distanceToTarget < bestDistance) {
                target = bibble;
                bestDistance = distanceToTarget;
            }
        }
        if (target == null || target.getStats() == null) {
            return false;
        }
        float effectiveDamage = Math.max(1f, damage - target.getStats().getPhysicalDefense() * 0.25f);
        target.getStats().setHealth(Math.max(0f, target.getStats().getHealth() - effectiveDamage));
        if (target.getStats().getHealth() <= 0f) {
            target.setLifecycleState(settings.isBibblePermanentDeathEnabled() ? BibbleLifecycleState.DEAD : BibbleLifecycleState.INCAPACITATED);
            target.setCurrentCommand(BibbleCommandType.STAY);
        } else if (target.getLifecycleState() == BibbleLifecycleState.WILD) {
            target.setFearLove(target.getFearLove() - 3);
        }
        return true;
    }

    private boolean captureNearestWildBibble(String clientId, BibbleInventory inventory, EntityState player, PlayerInventory playerInventory) {
        if (!settings.isBibbleCapturingEnabled() || player == null || playerInventory == null) {
            return false;
        }
        BibbleStateDto target = findNearestWild(player, 2);
        if (target == null) {
            return false;
        }

        ItemStackDto selectedItem = playerInventory.getSelectedItem();
        String selectedType = selectedItem == null ? "" : ItemCatalog.normalizeType(selectedItem.getItemType());
        if (!ItemCatalog.isCaptureItem(selectedType)) {
            return false;
        }

        float itemModifier = ItemCatalog.getCaptureModifier(selectedType);
        float healthRatio = target.getStats() == null || target.getStats().getMaxHealth() <= 0f
                ? 1f
                : Math.max(0f, Math.min(1f, target.getStats().getHealth() / target.getStats().getMaxHealth()));
        float woundedBonus = (1f - healthRatio) * 30f;
        float relationshipBonus = target.getConnection() * 0.10f + target.getFearLove() * 0.05f;
        float difficulty = Math.max(10f, target.getCaptureScore());
        float score = 35f + itemModifier + woundedBonus + relationshipBonus - (difficulty * 0.35f);

        // deterministic enough for server tests, variable enough for first playtests
        float roll = Math.floorMod((int) (seed + target.getBibbleId().hashCode() + player.getTileX() * 31 + player.getTileY() * 17), 100);
        playerInventory.removeFromSlot(playerInventory.getSelectedSlot(), 1);
        if (score < roll) {
            target.setFearLove(target.getFearLove() - 5);
            return true;
        }

        target.setOwnerClientId(clientId);
        target.setFearLove(Math.max(10, target.getFearLove() + 20));
        target.setWorkstationEntityId(null);
        target.setFencedAreaId(null);
        target.setCurrentCommand(BibbleCommandType.FOLLOW);
        if (inventory.activeIds().size() < settings.getMaxActiveBibbles()) {
            int slot = firstFreeSlot(inventory);
            inventory.activate(target.getBibbleId(), slot);
            target.setTeamSlot(slot);
            target.setLifecycleState(BibbleLifecycleState.OWNED_ACTIVE_FOLLOWING);
            setBibblePositionNearPlayer(target, player, slot);
        } else {
            inventory.storeInTerminal(target.getBibbleId());
            target.setTeamSlot(-1);
            target.setLifecycleState(BibbleLifecycleState.OWNED_STORED_TERMINAL);
        }
        return true;
    }

    private BibbleStateDto findNearestWild(EntityState player, int maxDistance) {
        int playerX = player.getTileX() == null ? 0 : player.getTileX();
        int playerY = player.getTileY() == null ? 0 : player.getTileY();
        BibbleStateDto best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (BibbleStateDto bibble : bibblesById.values()) {
            if (bibble == null || bibble.getLifecycleState() != BibbleLifecycleState.WILD) {
                continue;
            }
            int distance = Math.abs(playerX - bibble.getTileX()) + Math.abs(playerY - bibble.getTileY());
            if (distance <= maxDistance && distance < bestDistance) {
                best = bibble;
                bestDistance = distance;
            }
        }
        return best;
    }

    private int firstFreeSlot(BibbleInventory inventory) {
        for (int i = 0; i < settings.getMaxActiveBibbles(); i++) {
            if (inventory.getActiveAt(i) == null) {
                return i;
            }
        }
        return Math.max(0, settings.getMaxActiveBibbles() - 1);
    }

    private String firstActiveOwnedId(String clientId, BibbleInventory inventory) {
        for (String bibbleId : inventory.activeIds()) {
            BibbleStateDto bibble = bibblesById.get(bibbleId);
            if (bibble != null && bibble.isOwnedBy(clientId)) {
                return bibbleId;
            }
        }
        return null;
    }

    private EntityState findNearestWorkstation(EntityState player, List<EntityState> buildings) {
        if (player == null || buildings == null) {
            return null;
        }
        int playerX = player.getTileX() == null ? 0 : player.getTileX();
        int playerY = player.getTileY() == null ? 0 : player.getTileY();
        EntityState best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (EntityState building : buildings) {
            if (building == null || building.getTileX() == null || building.getTileY() == null) {
                continue;
            }
            String type = normalize(building.getBuildingType());
            if (!WorkstationCatalog.canAcceptBibbleWorker(type)) {
                continue;
            }
            int distance = Math.abs(building.getTileX() - playerX) + Math.abs(building.getTileY() - playerY);
            if (distance <= 2 && distance < bestDistance) {
                best = building;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void setBibblePositionNearPlayer(BibbleStateDto bibble, EntityState player, int slot) {
        if (player == null) {
            moveBibbleTo(bibble, slot, 0);
            return;
        }
        int ownerX = player.getTileX() == null ? 0 : player.getTileX();
        int ownerY = player.getTileY() == null ? 0 : player.getTileY();
        setBibblePositionAround(bibble, ownerX, ownerY, slot);
    }

    private void setBibblePositionAround(BibbleStateDto bibble, int ownerX, int ownerY, int slot) {
        int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}};
        int index = Math.max(0, Math.min(offsets.length - 1, slot));
        moveBibbleTo(bibble, ownerX + offsets[index][0], ownerY + offsets[index][1]);
    }

    private void moveBibbleTo(BibbleStateDto bibble, int tileX, int tileY) {
        bibble.setTileX(tileX);
        bibble.setTileY(tileY);
        bibble.setPosition(new Vector2f(tileX + 0.5f, tileY + 0.5f));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
