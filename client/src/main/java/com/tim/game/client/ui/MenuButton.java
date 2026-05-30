package com.tim.game.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class MenuButton {
    private final Rectangle bounds;
    private final String label;
    private final Runnable action;
    private boolean hovered;

    public MenuButton(float x, float y, float width, float height, String label, Runnable action) {
        this.bounds = new Rectangle(x, y, width, height);
        this.label = label;
        this.action = action;
    }

    public boolean containsScreenPoint(int screenX, int screenY) {
        float y = Gdx.graphics.getHeight() - screenY;
        return bounds.contains(screenX, y);
    }

    public void trigger() {
        if (action != null) {
            action.run();
        }
    }

    public void updateHover() {
        hovered = containsScreenPoint(Gdx.input.getX(), Gdx.input.getY());
    }

    public void drawShape(ShapeRenderer shapes) {
        shapes.setColor(hovered ? new Color(0.28f, 0.55f, 0.90f, 0.92f) : new Color(0.16f, 0.22f, 0.32f, 0.88f));
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        shapes.setColor(new Color(0.65f, 0.80f, 1.0f, 0.95f));
        shapes.rectLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y, 1.5f);
        shapes.rectLine(bounds.x, bounds.y + bounds.height, bounds.x + bounds.width, bounds.y + bounds.height, 1.5f);
        shapes.rectLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height, 1.5f);
        shapes.rectLine(bounds.x + bounds.width, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 1.5f);
    }

    public void drawText(SpriteBatch batch, BitmapFont font) {
        GlyphLayout layout = new GlyphLayout(font, label);
        font.setColor(Color.WHITE);
        font.draw(batch,
                label,
                bounds.x + (bounds.width - layout.width) * 0.5f,
                bounds.y + (bounds.height + layout.height) * 0.5f);
    }
}
