package com.tim.game.server.world.inventory;

import com.tim.game.shared.DTOs.update.InventorySlotDto;
import com.tim.game.shared.DTOs.update.ItemStackDto;
import com.tim.game.shared.inventory.ItemCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-autoritatives Slot-Inventar eines Spielers.
 */
public class PlayerInventory {
    public static final int DEFAULT_SLOT_COUNT = 16;

    private final List<ItemStackDto> slots = new ArrayList<>();
    private int selectedSlot;

    public PlayerInventory() {
        this(DEFAULT_SLOT_COUNT);
    }

    public PlayerInventory(int slotCount) {
        for (int i = 0; i < slotCount; i++) {
            slots.add(null);
        }
    }

    public static PlayerInventory starterInventory() {
        PlayerInventory inventory = new PlayerInventory();
        for (ItemStackDto stack : ItemCatalog.createStarterStacks()) {
            inventory.addStack(stack);
        }
        return inventory;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void selectSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return;
        }
        selectedSlot = slotIndex;
    }

    public ItemStackDto getSelectedItem() {
        return getSlot(selectedSlot);
    }

    public ItemStackDto getSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return null;
        }
        ItemStackDto stack = slots.get(slotIndex);
        return stack == null || stack.isEmpty() ? null : stack;
    }

    public int addStack(ItemStackDto incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return 0;
        }

        String itemType = ItemCatalog.normalizeType(incoming.getItemType());
        int remaining = incoming.getQuantity();
        int maxStack = ItemCatalog.getMaxStack(itemType);

        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStackDto current = slots.get(i);
            if (current == null || current.isEmpty()) {
                continue;
            }
            if (!itemType.equals(ItemCatalog.normalizeType(current.getItemType()))) {
                continue;
            }

            int freeSpace = Math.max(0, maxStack - current.getQuantity());
            int moved = Math.min(freeSpace, remaining);
            current.setQuantity(current.getQuantity() + moved);
            remaining -= moved;
        }

        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStackDto current = slots.get(i);
            if (current != null && !current.isEmpty()) {
                continue;
            }

            int moved = Math.min(maxStack, remaining);
            slots.set(i, ItemCatalog.createStack(itemType, moved));
            remaining -= moved;
        }

        return remaining;
    }

    public ItemStackDto removeFromSlot(int slotIndex, int amount) {
        ItemStackDto current = getSlot(slotIndex);
        if (current == null) {
            return null;
        }

        int normalizedAmount = Math.max(1, Math.min(amount, current.getQuantity()));
        ItemStackDto removed = ItemCatalog.createStack(current.getItemType(), normalizedAmount);
        current.setQuantity(current.getQuantity() - normalizedAmount);
        if (current.getQuantity() <= 0) {
            slots.set(slotIndex, null);
        }
        return removed;
    }

    public boolean consumeItem(String itemType, int amount) {
        String normalizedType = ItemCatalog.normalizeType(itemType);
        int required = Math.max(1, amount);
        if (countItem(normalizedType) < required) {
            return false;
        }

        int remaining = required;
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStackDto current = slots.get(i);
            if (current == null || current.isEmpty()) {
                continue;
            }
            if (!normalizedType.equals(ItemCatalog.normalizeType(current.getItemType()))) {
                continue;
            }

            int removed = Math.min(current.getQuantity(), remaining);
            current.setQuantity(current.getQuantity() - removed);
            remaining -= removed;
            if (current.getQuantity() <= 0) {
                slots.set(i, null);
            }
        }

        return true;
    }

    public int countItem(String itemType) {
        String normalizedType = ItemCatalog.normalizeType(itemType);
        int count = 0;
        for (ItemStackDto stack : slots) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (normalizedType.equals(ItemCatalog.normalizeType(stack.getItemType()))) {
                count += stack.getQuantity();
            }
        }
        return count;
    }

    public List<InventorySlotDto> toDtoSlots() {
        List<InventorySlotDto> result = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            ItemStackDto stack = slots.get(i);
            result.add(new InventorySlotDto(i, stack == null ? null : stack.copy(), i == selectedSlot));
        }
        return result;
    }
}
