package com.tim.game.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.client.texture.TexturePackManager;
import com.tim.game.shared.DTOs.update.InventorySlotDto;
import com.tim.game.shared.DTOs.update.ItemStackDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;

import java.util.List;

public class InventoryHudRenderer {
    private static final int HOTBAR_SLOT_COUNT = 8;
    private static final float SLOT_SIZE = 54f;
    private static final float SLOT_GAP = 7f;
    private static final float ICON_SIZE = 30f;

    private final TexturePackManager texturePackManager = TexturePackManager.getInstance();

    public void render(ShapeRenderer shapes,
                       SpriteBatch batch,
                       BitmapFont font,
                       OrthographicCamera hudCamera,
                       WorldSnapshotDto snapshot,
                       String localClientId,
                       boolean expanded) {
        PlayerStateDto localPlayer = findLocalPlayer(snapshot, localClientId);
        if (localPlayer == null) {
            return;
        }

        List<InventorySlotDto> slots = localPlayer.getInventorySlots();
        if (slots == null || slots.isEmpty()) {
            return;
        }

        shapes.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        if (expanded) {
            renderInventoryPanel(shapes, batch, font, hudCamera, localPlayer, slots);
        }
        renderHotbar(shapes, batch, font, hudCamera, localPlayer, slots);
    }

    private void renderHotbar(ShapeRenderer shapes,
                              SpriteBatch batch,
                              BitmapFont font,
                              OrthographicCamera hudCamera,
                              PlayerStateDto player,
                              List<InventorySlotDto> slots) {
        int visibleSlots = Math.min(HOTBAR_SLOT_COUNT, slots.size());
        float totalWidth = visibleSlots * SLOT_SIZE + (visibleSlots - 1) * SLOT_GAP;
        float startX = (hudCamera.viewportWidth - totalWidth) * 0.5f;
        float startY = 18f;

        renderSlotBackgrounds(shapes, slots, visibleSlots, startX, startY, SLOT_SIZE, player.getSelectedInventorySlot());
        renderSlotContents(batch, font, slots, visibleSlots, startX, startY, SLOT_SIZE, true);
        renderSelectedItemHint(batch, font, player, slots, startX, startY + SLOT_SIZE + 19f);
    }

    private void renderInventoryPanel(ShapeRenderer shapes,
                                      SpriteBatch batch,
                                      BitmapFont font,
                                      OrthographicCamera hudCamera,
                                      PlayerStateDto player,
                                      List<InventorySlotDto> slots) {
        int columns = 4;
        int rows = (int) Math.ceil(slots.size() / (float) columns);
        float panelPadding = 18f;
        float panelWidth = columns * SLOT_SIZE + (columns - 1) * SLOT_GAP + panelPadding * 2f;
        float panelHeight = rows * SLOT_SIZE + (rows - 1) * SLOT_GAP + panelPadding * 2f + 44f;
        float panelX = hudCamera.viewportWidth - panelWidth - 24f;
        float panelY = hudCamera.viewportHeight - panelHeight - 56f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.02f, 0.025f, 0.035f, 0.84f));
        shapes.rect(panelX, panelY, panelWidth, panelHeight);
        shapes.setColor(new Color(1f, 1f, 1f, 0.08f));
        shapes.rect(panelX + 2f, panelY + 2f, panelWidth - 4f, panelHeight - 4f);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(1f, 1f, 1f, 0.22f));
        shapes.rect(panelX, panelY, panelWidth, panelHeight);
        shapes.end();

        batch.begin();
        font.getData().setScale(0.95f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Inventory", panelX + panelPadding, panelY + panelHeight - 18f);
        font.getData().setScale(0.70f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "I: close | 1-8 select | E pickup | F use | Q drop", panelX + panelPadding, panelY + panelHeight - 38f);
        batch.end();

        float gridX = panelX + panelPadding;
        float gridTopY = panelY + panelHeight - panelPadding - 50f;
        renderPanelSlots(shapes, slots, gridX, gridTopY, columns, player.getSelectedInventorySlot());
        renderPanelContents(batch, font, slots, gridX, gridTopY, columns);
    }

    private void renderSlotBackgrounds(ShapeRenderer shapes,
                                       List<InventorySlotDto> slots,
                                       int visibleSlots,
                                       float startX,
                                       float startY,
                                       float slotSize,
                                       int selectedSlot) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < visibleSlots; i++) {
            InventorySlotDto slot = slots.get(i);
            boolean selected = slot != null && slot.getSlotIndex() == selectedSlot;
            float x = startX + i * (slotSize + SLOT_GAP);
            shapes.setColor(selected ? new Color(0.95f, 0.62f, 0.88f, 0.82f) : new Color(0.035f, 0.04f, 0.055f, 0.78f));
            shapes.rect(x, startY, slotSize, slotSize);
            shapes.setColor(new Color(1f, 1f, 1f, selected ? 0.18f : 0.08f));
            shapes.rect(x + 3f, startY + 3f, slotSize - 6f, slotSize - 6f);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < visibleSlots; i++) {
            InventorySlotDto slot = slots.get(i);
            boolean selected = slot != null && slot.getSlotIndex() == selectedSlot;
            float x = startX + i * (slotSize + SLOT_GAP);
            shapes.setColor(selected ? Color.WHITE : new Color(1f, 1f, 1f, 0.25f));
            shapes.rect(x, startY, slotSize, slotSize);
        }
        shapes.end();
    }

    private void renderSlotContents(SpriteBatch batch,
                                    BitmapFont font,
                                    List<InventorySlotDto> slots,
                                    int visibleSlots,
                                    float startX,
                                    float startY,
                                    float slotSize,
                                    boolean showKeyLabel) {
        batch.begin();
        for (int i = 0; i < visibleSlots; i++) {
            InventorySlotDto slot = slots.get(i);
            if (slot == null) {
                continue;
            }
            float x = startX + i * (slotSize + SLOT_GAP);
            renderSingleSlotContent(batch, font, slot, x, startY, slotSize, showKeyLabel ? String.valueOf(i + 1) : null);
        }
        batch.end();
    }

    private void renderPanelSlots(ShapeRenderer shapes,
                                  List<InventorySlotDto> slots,
                                  float gridX,
                                  float gridTopY,
                                  int columns,
                                  int selectedSlot) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (InventorySlotDto slot : slots) {
            int row = slot.getSlotIndex() / columns;
            int col = slot.getSlotIndex() % columns;
            float x = gridX + col * (SLOT_SIZE + SLOT_GAP);
            float y = gridTopY - row * (SLOT_SIZE + SLOT_GAP) - SLOT_SIZE;
            boolean selected = slot.getSlotIndex() == selectedSlot;
            shapes.setColor(selected ? new Color(0.95f, 0.62f, 0.88f, 0.82f) : new Color(0.035f, 0.04f, 0.055f, 0.86f));
            shapes.rect(x, y, SLOT_SIZE, SLOT_SIZE);
            shapes.setColor(new Color(1f, 1f, 1f, selected ? 0.16f : 0.07f));
            shapes.rect(x + 3f, y + 3f, SLOT_SIZE - 6f, SLOT_SIZE - 6f);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (InventorySlotDto slot : slots) {
            int row = slot.getSlotIndex() / columns;
            int col = slot.getSlotIndex() % columns;
            float x = gridX + col * (SLOT_SIZE + SLOT_GAP);
            float y = gridTopY - row * (SLOT_SIZE + SLOT_GAP) - SLOT_SIZE;
            shapes.setColor(slot.getSlotIndex() == selectedSlot ? Color.WHITE : new Color(1f, 1f, 1f, 0.24f));
            shapes.rect(x, y, SLOT_SIZE, SLOT_SIZE);
        }
        shapes.end();
    }

    private void renderPanelContents(SpriteBatch batch,
                                     BitmapFont font,
                                     List<InventorySlotDto> slots,
                                     float gridX,
                                     float gridTopY,
                                     int columns) {
        batch.begin();
        for (InventorySlotDto slot : slots) {
            int row = slot.getSlotIndex() / columns;
            int col = slot.getSlotIndex() % columns;
            float x = gridX + col * (SLOT_SIZE + SLOT_GAP);
            float y = gridTopY - row * (SLOT_SIZE + SLOT_GAP) - SLOT_SIZE;
            renderSingleSlotContent(batch, font, slot, x, y, SLOT_SIZE, null);
        }
        batch.end();
    }

    private void renderSingleSlotContent(SpriteBatch batch,
                                         BitmapFont font,
                                         InventorySlotDto slot,
                                         float x,
                                         float y,
                                         float slotSize,
                                         String keyLabel) {
        ItemStackDto item = slot.getItem();
        if (item != null && !item.isEmpty()) {
            Texture texture = texturePackManager.getItemTexture(item.getItemType());
            float iconX = x + (slotSize - ICON_SIZE) * 0.5f;
            float iconY = y + 17f;
            batch.draw(texture, iconX, iconY, ICON_SIZE, ICON_SIZE);

            font.getData().setScale(0.66f);
            font.setColor(Color.WHITE);
            font.draw(batch, String.valueOf(item.getQuantity()), x + slotSize - 17f, y + 16f);
        }

        if (keyLabel != null) {
            font.getData().setScale(0.58f);
            font.setColor(new Color(1f, 1f, 1f, 0.72f));
            font.draw(batch, keyLabel, x + 5f, y + slotSize - 5f);
        }
    }

    private void renderSelectedItemHint(SpriteBatch batch,
                                        BitmapFont font,
                                        PlayerStateDto player,
                                        List<InventorySlotDto> slots,
                                        float startX,
                                        float y) {
        InventorySlotDto selectedSlot = null;
        for (InventorySlotDto slot : slots) {
            if (slot != null && slot.getSlotIndex() == player.getSelectedInventorySlot()) {
                selectedSlot = slot;
                break;
            }
        }

        if (selectedSlot == null || selectedSlot.getItem() == null || selectedSlot.getItem().isEmpty()) {
            return;
        }

        ItemStackDto item = selectedSlot.getItem();
        batch.begin();
        font.getData().setScale(0.74f);
        font.setColor(Color.WHITE);
        String suffix = item.isPlaceable() ? " | Right click: place" : item.isUsable() ? " | F: use" : "";
        font.draw(batch, item.getDisplayName() + " x" + item.getQuantity() + suffix, startX, y);
        batch.end();
    }

    private PlayerStateDto findLocalPlayer(WorldSnapshotDto snapshot, String localClientId) {
        if (snapshot == null || snapshot.getPlayers() == null || localClientId == null) {
            return null;
        }

        for (PlayerStateDto player : snapshot.getPlayers()) {
            if (player == null) {
                continue;
            }
            if (localClientId.equals(player.getClientId())) {
                return player;
            }
        }
        return null;
    }
}
