package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.client.ClientGame;
import com.tim.game.client.ui.MenuButton;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMenuScreen extends ScreenAdapter {
    protected final ClientGame game;
    protected final List<MenuButton> buttons = new ArrayList<>();
    protected ShapeRenderer shapes;
    protected SpriteBatch batch;
    protected BitmapFont font;
    protected String message = "";
    protected Color messageColor = Color.LIGHT_GRAY;

    protected AbstractMenuScreen(ClientGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
    }

    protected void clearBackground() {
        Gdx.gl.glClearColor(0.055f, 0.06f, 0.085f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    protected void drawPanel(float x, float y, float width, float height) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.08f, 0.10f, 0.15f, 0.95f));
        shapes.rect(x, y, width, height);
        shapes.setColor(new Color(0.24f, 0.34f, 0.48f, 0.95f));
        shapes.rectLine(x, y, x + width, y, 1.5f);
        shapes.rectLine(x, y + height, x + width, y + height, 1.5f);
        shapes.rectLine(x, y, x, y + height, 1.5f);
        shapes.rectLine(x + width, y, x + width, y + height, 1.5f);
        shapes.end();
    }

    protected void drawTitle(String title, String subtitle) {
        batch.begin();
        font.getData().setScale(2.2f);
        font.setColor(Color.WHITE);
        font.draw(batch, title, 60f, Gdx.graphics.getHeight() - 60f);
        font.getData().setScale(1.1f);
        font.setColor(new Color(0.70f, 0.78f, 0.88f, 1f));
        if (subtitle != null && !subtitle.isBlank()) {
            font.draw(batch, subtitle, 63f, Gdx.graphics.getHeight() - 96f);
        }
        batch.end();
    }

    protected void drawButtons() {
        for (MenuButton button : buttons) {
            button.updateHover();
        }
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (MenuButton button : buttons) {
            button.drawShape(shapes);
        }
        shapes.end();

        batch.begin();
        font.getData().setScale(1.15f);
        for (MenuButton button : buttons) {
            button.drawText(batch, font);
        }
        batch.end();
    }

    protected void drawMessage(float x, float y) {
        if (message == null || message.isBlank()) {
            return;
        }
        batch.begin();
        font.getData().setScale(1.0f);
        font.setColor(messageColor);
        font.draw(batch, message, x, y);
        batch.end();
    }

    protected boolean handleButtonClick(int screenX, int screenY) {
        for (MenuButton button : buttons) {
            if (button.containsScreenPoint(screenX, screenY)) {
                button.trigger();
                return true;
            }
        }
        return false;
    }

    protected float centerX(float width) {
        return (Gdx.graphics.getWidth() - width) * 0.5f;
    }

    @Override
    public void dispose() {
        if (shapes != null) {
            shapes.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
