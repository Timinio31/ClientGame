package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.tim.game.client.ClientGame;
import com.tim.game.client.config.LocalWorldProfile;
import com.tim.game.client.config.LocalWorldRepository;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.ui.MenuButton;
import com.tim.game.shared.config.WorldSettings;

import java.util.List;

public class LocalWorldsScreen extends AbstractMenuScreen {
    private final LocalWorldRepository repository = new LocalWorldRepository();
    private List<LocalWorldProfile> worlds;

    public LocalWorldsScreen(ClientGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();
        rebuildButtons();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return handleButtonClick(screenX, screenY);
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    game.showMainMenu();
                    return true;
                }
                return false;
            }
        });
    }

    private void rebuildButtons() {
        worlds = repository.loadWorlds();
        buttons.clear();

        float width = 900f;
        float height = 44f;
        float x = centerX(width);
        float y = 435f;
        float gap = 52f;

        int maxVisible = Math.min(7, worlds.size());
        for (int i = 0; i < maxVisible; i++) {
            LocalWorldProfile profile = worlds.get(i);
            String label = buildWorldLabel(profile);
            buttons.add(new MenuButton(x, y - gap * i, width, height, label, () -> playWorld(profile)));
        }

        buttons.add(new MenuButton(80f, 65f, 250f, 48f, "Create New World", () -> game.setScreen(new CreateLocalWorldScreen(game))));
        buttons.add(new MenuButton(350f, 65f, 190f, 48f, "Refresh", this::rebuildButtons));
        buttons.add(new MenuButton(560f, 65f, 190f, 48f, "Back", game::showMainMenu));

        if (worlds.isEmpty()) {
            messageColor = Color.LIGHT_GRAY;
            message = "No local worlds yet. Create one first. Save folder: " + repository.getBaseDirectory();
        } else {
            messageColor = Color.LIGHT_GRAY;
            message = "Click a world to mark it active and connect to local room " + LocalWorldRepository.LOCAL_ROOM_ID
                    + ". Active marker: " + repository.getActiveWorldFile();
        }
    }

    private String buildWorldLabel(LocalWorldProfile profile) {
        WorldSettings settings = profile.getWorldSettings();
        String played = settings.getLastPlayedAt() == null || settings.getLastPlayedAt().isBlank()
                ? "never"
                : settings.getLastPlayedAt();
        return settings.getWorldName()
                + "  |  " + settings.getMapWidth() + "x" + settings.getMapHeight()
                + "  |  seed " + settings.getSeed()
                + "  |  " + played;
    }

    private void playWorld(LocalWorldProfile profile) {
        try {
            LocalWorldProfile selected = repository.selectForLocalPlay(profile);
            ClientConfig config = repository.buildLocalClientConfig(ClientGame.newClientId());
            System.out.println("[Client] Selected local world " + selected.getWorldId()
                    + " settings=" + selected.getSettingsFile()
                    + " map=" + selected.getMapFile());
            game.startGame(config);
        } catch (Exception e) {
            messageColor = new Color(1f, 0.45f, 0.45f, 1f);
            message = "Could not start local world: " + e.getMessage();
        }
    }

    @Override
    public void render(float delta) {
        clearBackground();
        drawTitle("Local Worlds", "Choose a saved local world. The server loads the selected world from ~/.clientgame/worlds.");
        drawPanel(50f, 50f, Gdx.graphics.getWidth() - 100f, Gdx.graphics.getHeight() - 165f);

        batch.begin();
        font.getData().setScale(0.95f);
        font.setColor(new Color(0.72f, 0.80f, 0.90f, 1f));
        font.draw(batch, "For normal local play start the server after selecting a world, or restart it when switching worlds.", 80f, 138f);
        font.draw(batch, "Default local room stays '1' so .\\gradlew.bat :server:run remains compatible.", 80f, 118f);
        batch.end();

        drawButtons();
        drawMessage(80f, 160f);
    }
}
