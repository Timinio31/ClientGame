package com.tim.game.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.bibble.BibbleInventoryDto;
import com.tim.game.shared.bibble.BibbleStateDto;
import com.tim.game.shared.bibble.BibbleStats;
import com.tim.game.shared.config.WorldSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * First Bibble inventory/detail menu. It only displays state and sends no decisions itself.
 */
public class BibbleMenuRenderer {

    public void render(ShapeRenderer shapes,
                       SpriteBatch batch,
                       BitmapFont font,
                       OrthographicCamera hudCamera,
                       WorldSnapshotDto snapshot,
                       String clientId,
                       WorldSettings settings,
                       boolean open) {
        if (!open || !isBibbleMenuAllowed(settings)) {
            return;
        }

        PlayerStateDto player = findLocalPlayer(snapshot, clientId);
        if (player == null) {
            return;
        }
        BibbleInventoryDto inventory = player.getBibbleInventory();
        Map<String, BibbleStateDto> bibblesById = bibblesById(snapshot);

        float width = Math.min(780f, Gdx.graphics.getWidth() - 80f);
        float height = Math.min(500f, Gdx.graphics.getHeight() - 105f);
        float x = 42f;
        float y = Gdx.graphics.getHeight() - height - 52f;

        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.035f, 0.030f, 0.050f, 0.96f));
        shapes.rect(x, y, width, height);
        shapes.setColor(new Color(0.34f, 0.26f, 0.52f, 1f));
        shapes.rect(x, y + height - 42f, width, 42f);
        shapes.setColor(new Color(0.12f, 0.12f, 0.18f, 0.92f));
        shapes.rect(x + 18f, y + 18f, width - 36f, height - 78f);
        shapes.end();

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(1.15f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Bibble Inventory", x + 18f, y + height - 15f);
        font.getData().setScale(0.74f);
        font.setColor(new Color(0.86f, 0.88f, 0.96f, 1f));
        font.draw(batch, "B closes | 1-3 toggle | C capture nearest with selected capture item | F follow | S stay | P patrol | R recall | W assign workstation", x + 185f, y + height - 17f);

        float leftY = y + height - 76f;
        font.getData().setScale(0.92f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Active Team (max " + (inventory == null ? settings.getMaxActiveBibbles() : inventory.getMaxActiveSlots()) + ")", x + 36f, leftY);
        leftY -= 28f;

        BibbleStateDto firstActive = null;
        for (int i = 0; i < settings.getMaxActiveBibbles(); i++) {
            String bibbleId = getActiveIdAt(inventory, i);
            BibbleStateDto bibble = bibblesById.get(bibbleId);
            if (firstActive == null && bibble != null) {
                firstActive = bibble;
            }
            renderBibbleLine(batch, font, x + 54f, leftY, (i + 1) + ".", bibble, "empty active slot");
            leftY -= 30f;
        }

        leftY -= 12f;
        font.getData().setScale(0.92f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Terminal / Storage", x + 36f, leftY);
        leftY -= 28f;
        int storedShown = 0;
        if (inventory != null && inventory.getStoredBibbleIds() != null) {
            for (String id : inventory.getStoredBibbleIds()) {
                if (storedShown >= 5) {
                    break;
                }
                renderBibbleLine(batch, font, x + 54f, leftY, "-", bibblesById.get(id), "stored unknown bibble");
                leftY -= 26f;
                storedShown++;
            }
        }
        if (storedShown == 0) {
            font.getData().setScale(0.76f);
            font.setColor(new Color(0.68f, 0.72f, 0.82f, 1f));
            font.draw(batch, "No Bibbles in terminal storage.", x + 54f, leftY);
        }

        float rightX = x + width * 0.52f;
        float detailY = y + height - 76f;
        renderDetail(batch, font, rightX, detailY, firstActive, settings);

        font.getData().setScale(0.68f);
        font.setColor(new Color(0.74f, 0.78f, 0.88f, 1f));
        font.draw(batch, "Capture V1: select a capture item, stand near a wild Bibble, press B then C. TODO: breeding, trade, fenced zones, route network and workstation recipe UI.", x + 36f, y + 38f);
        batch.end();
    }

    public static boolean isBibbleMenuAllowed(WorldSettings settings) {
        return settings != null && settings.isBibblesEnabled();
    }

    private void renderBibbleLine(SpriteBatch batch, BitmapFont font, float x, float y, String prefix, BibbleStateDto bibble, String emptyText) {
        font.getData().setScale(0.78f);
        if (bibble == null) {
            font.setColor(new Color(0.58f, 0.60f, 0.68f, 1f));
            font.draw(batch, prefix + " " + emptyText, x, y);
            return;
        }
        font.setColor(Color.WHITE);
        font.draw(batch, prefix + " " + safe(bibble.getCustomName()) + " [" + bibble.getPrimaryType() + "]", x, y);
        font.setColor(new Color(0.72f, 0.78f, 0.88f, 1f));
        font.draw(batch, bibble.getLifecycleState() + " | " + bibble.getCurrentCommand(), x + 220f, y);
    }

    private void renderDetail(SpriteBatch batch, BitmapFont font, float x, float y, BibbleStateDto bibble, WorldSettings settings) {
        font.getData().setScale(0.95f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Selected Bibble Detail", x, y);
        y -= 30f;
        if (bibble == null) {
            font.getData().setScale(0.76f);
            font.setColor(new Color(0.70f, 0.74f, 0.84f, 1f));
            font.draw(batch, "No active Bibble selected. Put one into an active slot with 1-3.", x, y);
            return;
        }

        BibbleStats stats = bibble.getStats() == null ? new BibbleStats() : bibble.getStats();
        font.getData().setScale(0.78f);
        font.setColor(new Color(0.86f, 0.90f, 0.98f, 1f));
        font.draw(batch, "Name: " + safe(bibble.getCustomName()) + " | Species: " + safe(bibble.getSpeciesId()), x, y); y -= 23f;
        font.draw(batch, "Type: " + bibble.getPrimaryType() + (bibble.getSecondaryType() == null ? "" : " / " + bibble.getSecondaryType()), x, y); y -= 23f;
        font.draw(batch, "Personality: " + bibble.getPersonality() + " | Level " + bibble.getLevel(), x, y); y -= 23f;
        font.draw(batch, "Bond scalar: " + bibble.getFearLove() + " (-100 fear, +100 love) | Connection: " + bibble.getConnection(), x, y); y -= 23f;
        font.draw(batch, "HP: " + round(stats.getHealth()) + "/" + round(stats.getMaxHealth()) + " | Speed: " + round(stats.getSpeed()) + " | Work: " + round(stats.getWorkSpeed()), x, y); y -= 23f;
        font.draw(batch, "PATK: " + round(stats.getPhysicalAttack()) + " | SATK: " + round(stats.getPsychicAttack())
                + " | PDEF: " + round(stats.getPhysicalDefense()) + " | SDEF: " + round(stats.getPsychicDefense()), x, y); y -= 23f;
        font.draw(batch, "Obedience: " + round(stats.getObedience()) + " | Rebellion: " + round(stats.getRebellion()) + " | Carry: " + round(stats.getCarryCapacity()), x, y); y -= 23f;
        font.draw(batch, "Capture score prepared: " + round(bibble.getCaptureScore()) + " | Frozen: " + bibble.isFrozen(), x, y); y -= 30f;

        font.setColor(Color.WHITE);
        font.draw(batch, "Attacks", x, y); y -= 22f;
        font.setColor(new Color(0.78f, 0.82f, 0.90f, 1f));
        if (bibble.getAttacks() == null || bibble.getAttacks().isEmpty()) {
            font.draw(batch, "No attacks learned.", x, y);
        } else {
            for (int i = 0; i < Math.min(4, bibble.getAttacks().size()); i++) {
                var attack = bibble.getAttacks().get(i);
                font.draw(batch, "- " + attack.getDisplayName() + " [" + attack.getType() + "] power=" + round(attack.getPower()) + " cd=" + attack.getCooldownTicks(), x, y);
                y -= 20f;
            }
        }

        y -= 8f;
        font.setColor(new Color(0.86f, 0.78f, 0.55f, 1f));
        font.draw(batch, "Settings: combat=" + settings.isBibbleCombatEnabled()
                + ", trading=" + settings.isBibbleTradingEnabled()
                + ", workstation=" + settings.isBibbleWorkstationAutomationEnabled(), x, y);
    }

    private PlayerStateDto findLocalPlayer(WorldSnapshotDto snapshot, String clientId) {
        if (snapshot == null || snapshot.getPlayers() == null || clientId == null) {
            return null;
        }
        for (PlayerStateDto player : snapshot.getPlayers()) {
            if (player != null && clientId.equals(player.getClientId())) {
                return player;
            }
        }
        return null;
    }

    private Map<String, BibbleStateDto> bibblesById(WorldSnapshotDto snapshot) {
        Map<String, BibbleStateDto> result = new HashMap<>();
        if (snapshot == null || snapshot.getBibbles() == null) {
            return result;
        }
        for (BibbleStateDto bibble : snapshot.getBibbles()) {
            if (bibble != null && bibble.getBibbleId() != null) {
                result.put(bibble.getBibbleId(), bibble);
            }
        }
        return result;
    }

    private String getActiveIdAt(BibbleInventoryDto inventory, int slot) {
        if (inventory == null || inventory.getActiveBibbleIds() == null || slot < 0 || slot >= inventory.getActiveBibbleIds().size()) {
            return null;
        }
        return inventory.getActiveBibbleIds().get(slot);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String round(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
