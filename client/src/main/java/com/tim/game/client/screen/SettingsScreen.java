package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.tim.game.client.ClientGame;
import com.tim.game.client.config.ClientSettings;
import com.tim.game.client.texture.TexturePackDefinition;
import com.tim.game.client.ui.MenuButton;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends AbstractMenuScreen {
    private final List<TexturePackDefinition> texturePacks = new ArrayList<>();
    private ClientSettings settings;
    private int selectedPackIndex;

    public SettingsScreen(ClientGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();
        settings = game.getClientSettings().normalizedCopy();
        reloadTexturePackList();

        buttons.clear();
        buttons.add(new MenuButton(centerX(660f), 305f, 200f, 48f, "Previous Pack", this::selectPreviousPack));
        buttons.add(new MenuButton(centerX(660f) + 230f, 305f, 200f, 48f, "Next Pack", this::selectNextPack));
        buttons.add(new MenuButton(centerX(660f) + 460f, 305f, 200f, 48f, "Apply", this::applySelectedPack));
        buttons.add(new MenuButton(centerX(660f), 235f, 315f, 48f, "Reload External Packs", this::reloadExternalPacks));
        buttons.add(new MenuButton(centerX(660f) + 345f, 235f, 315f, 48f, "Use Platinum Theme", this::selectAndApplyBuiltInPlatinum));
        buttons.add(new MenuButton(centerX(340f), 120f, 340f, 52f, "Back", game::showMainMenu));

        messageColor = Color.LIGHT_GRAY;
        message = "Texture pack selection is saved locally and used by the renderer immediately. Animated water and directional player sprites update inside the game.";

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return handleButtonClick(screenX, screenY);
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.LEFT) {
                    selectPreviousPack();
                    return true;
                }
                if (keycode == Input.Keys.RIGHT) {
                    selectNextPack();
                    return true;
                }
                if (keycode == Input.Keys.ENTER) {
                    applySelectedPack();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    game.showMainMenu();
                    return true;
                }
                return false;
            }
        });
    }

    private void reloadTexturePackList() {
        texturePacks.clear();
        texturePacks.addAll(game.getTexturePackManager().getAvailablePacks());
        if (texturePacks.isEmpty()) {
            selectedPackIndex = 0;
            return;
        }

        String selectedId = settings == null ? "" : settings.getTexturePackId();
        selectedPackIndex = 0;
        for (int i = 0; i < texturePacks.size(); i++) {
            if (texturePacks.get(i).getId().equals(selectedId)) {
                selectedPackIndex = i;
                return;
            }
        }
    }

    private void reloadExternalPacks() {
        game.getTexturePackManager().refreshAvailablePacks();
        reloadTexturePackList();
        messageColor = new Color(0.78f, 0.88f, 1.0f, 1f);
        message = "External texture packs reloaded from: " + game.getTexturePackManager().getExternalTexturePackFolder();
    }

    private void selectPreviousPack() {
        if (texturePacks.isEmpty()) {
            return;
        }
        selectedPackIndex = selectedPackIndex <= 0 ? texturePacks.size() - 1 : selectedPackIndex - 1;
    }

    private void selectNextPack() {
        if (texturePacks.isEmpty()) {
            return;
        }
        selectedPackIndex = (selectedPackIndex + 1) % texturePacks.size();
    }

    private void selectAndApplyBuiltInPlatinum() {
        for (int i = 0; i < texturePacks.size(); i++) {
            if ("midcentury_platinum".equals(texturePacks.get(i).getId())) {
                selectedPackIndex = i;
                applySelectedPack();
                return;
            }
        }
    }

    private void applySelectedPack() {
        TexturePackDefinition selected = getSelectedPack();
        if (selected == null) {
            return;
        }

        settings.setTexturePackId(selected.getId());
        game.saveClientSettings(settings);
        game.getTexturePackManager().setActivePack(selected.getId());
        messageColor = new Color(0.60f, 1.0f, 0.65f, 1f);
        message = "Applied texture pack: " + selected.getDisplayName();
    }

    private TexturePackDefinition getSelectedPack() {
        if (texturePacks.isEmpty()) {
            return null;
        }
        if (selectedPackIndex < 0 || selectedPackIndex >= texturePacks.size()) {
            selectedPackIndex = 0;
        }
        return texturePacks.get(selectedPackIndex);
    }

    @Override
    public void render(float delta) {
        clearBackground();
        drawTitle("Settings", "Select built-in or external texture packs. Arrow keys cycle, Enter applies.");
        drawPanel(centerX(780f), 100f, 780f, 430f);

        drawTexturePackInfo();
        drawMessage(centerX(720f), 190f);
        drawButtons();
    }

    private void drawTexturePackInfo() {
        TexturePackDefinition selected = getSelectedPack();
        TexturePackDefinition active = game.getTexturePackManager().getActivePack();

        batch.begin();
        font.getData().setScale(1.25f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Texture Packs", centerX(700f), 490f);

        font.getData().setScale(1.0f);
        font.setColor(new Color(0.78f, 0.84f, 0.92f, 1f));
        font.draw(batch, "Active: " + (active == null ? "none" : active.getDisplayName()), centerX(700f), 455f);

        if (selected != null) {
            font.setColor(new Color(1.0f, 0.92f, 0.65f, 1f));
            font.draw(batch, "Selected: " + selected.getDisplayName(), centerX(700f), 420f);
            font.setColor(new Color(0.82f, 0.86f, 0.92f, 1f));
            font.draw(batch, "ID: " + selected.getId(), centerX(700f), 390f);
            font.draw(batch, "Source: " + selected.getSourceLabel(), centerX(700f), 365f);
            font.draw(batch, selected.getDescription(), centerX(700f), 335f);
        }

        font.setColor(new Color(0.75f, 0.80f, 0.88f, 1f));
        font.draw(batch, "External packs folder: " + game.getTexturePackManager().getExternalTexturePackFolder(), centerX(700f), 270f);
        font.draw(batch, "Expected files: manifest.json, tiles/grass.png, water_0.png..water_2.png, wall.png, spawn.png, forest.png, mountain.png, road.png, entities/player_local_* / player_remote_* and building_*.png", centerX(700f), 245f);
        batch.end();
    }
}
