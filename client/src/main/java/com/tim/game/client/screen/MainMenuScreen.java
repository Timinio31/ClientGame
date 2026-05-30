package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.tim.game.client.ClientGame;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.ui.MenuButton;

public class MainMenuScreen extends AbstractMenuScreen {

    public MainMenuScreen(ClientGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();
        float width = 360f;
        float height = 52f;
        float x = centerX(width);
        float startY = 410f;
        float gap = 68f;

        buttons.clear();
        buttons.add(new MenuButton(x, startY, width, height, "Play Local", () ->
                game.startGame(ClientConfig.localDefault(ClientGame.newClientId()))));
        buttons.add(new MenuButton(x, startY - gap, width, height, "Play Multiplayer", () ->
                game.setScreen(new MultiplayerMenuScreen(game))));
        buttons.add(new MenuButton(x, startY - gap * 2, width, height, "Settings", game::showSettings));
        buttons.add(new MenuButton(x, startY - gap * 3, width, height, "Exit", () ->
                Gdx.app.exit()));

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return handleButtonClick(screenX, screenY);
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        clearBackground();
        drawTitle("ClientGame", "Server-authoritative multiplayer prototype");
        drawPanel(centerX(440f), 120f, 440f, 380f);
        drawButtons();
    }
}
