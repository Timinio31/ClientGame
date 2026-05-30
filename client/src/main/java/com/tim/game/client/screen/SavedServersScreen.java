package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.tim.game.client.ClientGame;
import com.tim.game.client.config.SavedServerRepository;
import com.tim.game.client.config.ServerProfile;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.ui.MenuButton;

import java.util.List;

public class SavedServersScreen extends AbstractMenuScreen {
    private final SavedServerRepository repository = new SavedServerRepository();
    private List<ServerProfile> profiles;

    public SavedServersScreen(ClientGame game) {
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
                    game.setScreen(new MultiplayerMenuScreen(game));
                    return true;
                }
                return false;
            }
        });
    }

    private void rebuildButtons() {
        profiles = repository.loadProfiles();
        buttons.clear();

        float width = 820f;
        float height = 44f;
        float x = centerX(width);
        float y = 430f;
        float gap = 52f;

        int maxVisible = Math.min(6, profiles.size());
        for (int i = 0; i < maxVisible; i++) {
            ServerProfile profile = profiles.get(i).normalizedCopy();
            String label = profile.getName() + "  |  " + profile.getHost() + ":" + profile.getPort() + "  |  Room " + profile.getRoomId();
            buttons.add(new MenuButton(x, y - gap * i, width, height, label, () -> connect(profile)));
        }

        buttons.add(new MenuButton(80f, 85f, 220f, 48f, "Direct Connect", () -> game.setScreen(new DirectConnectScreen(game))));
        buttons.add(new MenuButton(320f, 85f, 220f, 48f, "Refresh", this::rebuildButtons));
        buttons.add(new MenuButton(560f, 85f, 220f, 48f, "Clear List", () -> {
            repository.clear();
            messageColor = Color.LIGHT_GRAY;
            message = "Saved server list cleared.";
            rebuildButtons();
        }));
        buttons.add(new MenuButton(800f, 85f, 220f, 48f, "Back", () -> game.setScreen(new MultiplayerMenuScreen(game))));

        if (profiles.isEmpty()) {
            messageColor = Color.LIGHT_GRAY;
            message = "No saved servers yet. Use Direct Connect and choose 'Save + Connect'. File: " + repository.getSaveFile();
        } else {
            messageColor = Color.LIGHT_GRAY;
            message = "Click a saved profile to connect. Saved at: " + repository.getSaveFile();
        }
    }

    private void connect(ServerProfile profile) {
        repository.upsert(profile);
        ClientConfig config = ClientConfig.fromServerProfile(profile, ClientGame.newClientId());
        game.startGame(config);
    }

    @Override
    public void render(float delta) {
        clearBackground();
        drawTitle("Saved Servers", "Variant B: locally stored server profiles.");
        drawPanel(50f, 60f, Gdx.graphics.getWidth() - 100f, Gdx.graphics.getHeight() - 175f);
        drawButtons();
        drawMessage(80f, 160f);
    }
}
