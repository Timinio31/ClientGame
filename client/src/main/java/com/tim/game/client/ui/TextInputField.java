package com.tim.game.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class TextInputField {
    private final String label;
    private final Rectangle bounds;
    private String value;
    private final boolean password;
    private final boolean numericOnly;
    private boolean active;

    public TextInputField(String label, String value, float x, float y, float width, float height) {
        this(label, value, x, y, width, height, false, false);
    }

    public TextInputField(String label,
                          String value,
                          float x,
                          float y,
                          float width,
                          float height,
                          boolean password,
                          boolean numericOnly) {
        this.label = label;
        this.value = value == null ? "" : value;
        this.bounds = new Rectangle(x, y, width, height);
        this.password = password;
        this.numericOnly = numericOnly;
    }

    public boolean containsScreenPoint(int screenX, int screenY) {
        float y = Gdx.graphics.getHeight() - screenY;
        return bounds.contains(screenX, y);
    }

    public void keyTyped(char character) {
        if (!active) {
            return;
        }
        if (character == '\r' || character == '\n' || character == '\t') {
            return;
        }
        if (Character.isISOControl(character)) {
            return;
        }
        if (numericOnly && !Character.isDigit(character)) {
            return;
        }
        value += character;
    }

    public boolean keyDown(int keycode) {
        if (!active) {
            return false;
        }
        if (keycode == Input.Keys.BACKSPACE && !value.isEmpty()) {
            value = value.substring(0, value.length() - 1);
            return true;
        }
        if (keycode == Input.Keys.FORWARD_DEL) {
            value = "";
            return true;
        }
        return false;
    }

    public void drawShape(ShapeRenderer shapes) {
        shapes.setColor(active ? new Color(0.12f, 0.24f, 0.38f, 0.95f) : new Color(0.10f, 0.12f, 0.17f, 0.92f));
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapes.setColor(active ? new Color(0.55f, 0.78f, 1.0f, 1.0f) : new Color(0.35f, 0.42f, 0.52f, 1.0f));
        shapes.rectLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y, 1f);
        shapes.rectLine(bounds.x, bounds.y + bounds.height, bounds.x + bounds.width, bounds.y + bounds.height, 1f);
        shapes.rectLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height, 1f);
        shapes.rectLine(bounds.x + bounds.width, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 1f);
    }

    public void drawText(SpriteBatch batch, BitmapFont font) {
        font.setColor(new Color(0.72f, 0.80f, 0.90f, 1f));
        font.draw(batch, label, bounds.x, bounds.y + bounds.height + 18f);
        font.setColor(Color.WHITE);
        font.draw(batch, displayValue(), bounds.x + 10f, bounds.y + bounds.height * 0.62f);
    }

    private String displayValue() {
        if (!password) {
            return value;
        }
        return "*".repeat(value.length());
    }

    public String getValue() {
        return value == null ? "" : value.trim();
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
