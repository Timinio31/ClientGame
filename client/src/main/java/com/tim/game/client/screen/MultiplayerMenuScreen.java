package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.tim.game.client.ClientGame;
import com.tim.game.client.ui.MenuButton;

public class MultiplayerMenuScreen extends AbstractMenuScreen {

    public MultiplayerMenuScreen(ClientGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();
        float width = 420f;
        float height = 52f;
        float x = centerX(width);
        float startY = 410f;
        float gap = 68f;

        buttons.clear();
        buttons.add(new MenuButton(x, startY, width, height, "Direct Connect", () ->
                game.setScreen(new DirectConnectScreen(game))));
        buttons.add(new MenuButton(x, startY - gap, width, height, "Saved Servers", () ->
                game.setScreen(new SavedServersScreen(game))));
        buttons.add(new MenuButton(x, startY - gap * 2, width, height, "Create / Save Server Profile", () ->
                game.setScreen(new CreateServerScreen(game))));
        buttons.add(new MenuButton(x, startY - gap * 3, width, height, "Back", game::showMainMenu));

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

    @Override
    public void render(float delta) {
        clearBackground();
        drawTitle("Play Multiplayer", "Connect manually by host/IP or choose a saved server profile.");
        drawPanel(centerX(500f), 100f, 500f, 415f);
        drawButtons();
    }
}
