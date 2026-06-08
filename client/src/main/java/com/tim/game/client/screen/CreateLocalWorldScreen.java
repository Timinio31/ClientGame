package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.client.ClientGame;
import com.tim.game.client.config.LocalWorldProfile;
import com.tim.game.client.config.LocalWorldRepository;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.ui.MenuButton;
import com.tim.game.client.ui.TextInputField;

import java.util.ArrayList;
import java.util.List;

public class CreateLocalWorldScreen extends AbstractMenuScreen {
    private final LocalWorldRepository repository = new LocalWorldRepository();
    private final List<TextInputField> fields = new ArrayList<>();
    private int activeFieldIndex = 0;

    private TextInputField worldNameField;
    private TextInputField widthField;
    private TextInputField heightField;
    private TextInputField seedField;

    public CreateLocalWorldScreen(ClientGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();
        createFields();

        buttons.clear();
        buttons.add(new MenuButton(80f, 75f, 250f, 48f, "Create + Play", () -> createWorld(true)));
        buttons.add(new MenuButton(350f, 75f, 220f, 48f, "Create Only", () -> createWorld(false)));
        buttons.add(new MenuButton(590f, 75f, 190f, 48f, "Back", () -> game.setScreen(new LocalWorldsScreen(game))));

        setActiveField(0);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                for (int i = 0; i < fields.size(); i++) {
                    if (fields.get(i).containsScreenPoint(screenX, screenY)) {
                        setActiveField(i);
                        return true;
                    }
                }
                return handleButtonClick(screenX, screenY);
            }

            @Override
            public boolean keyTyped(char character) {
                fields.get(activeFieldIndex).keyTyped(character);
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (fields.get(activeFieldIndex).keyDown(keycode)) {
                    return true;
                }
                if (keycode == Input.Keys.TAB) {
                    setActiveField((activeFieldIndex + 1) % fields.size());
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    game.setScreen(new LocalWorldsScreen(game));
                    return true;
                }
                return false;
            }
        });
    }

    private void createFields() {
        fields.clear();
        float x = centerX(520f);
        float startY = 390f;
        float gap = 72f;
        float width = 520f;
        float height = 36f;

        worldNameField = new TextInputField("World name", "New World", x, startY, width, height);
        widthField = new TextInputField("Map width in tiles", "1024", x, startY - gap, width, height, false, true);
        heightField = new TextInputField("Map height in tiles", "1024", x, startY - gap * 2, width, height, false, true);
        seedField = new TextInputField("Seed (0 = generate from current time)", "0", x, startY - gap * 3, width, height, false, true);

        fields.add(worldNameField);
        fields.add(widthField);
        fields.add(heightField);
        fields.add(seedField);
    }

    private void setActiveField(int index) {
        activeFieldIndex = index;
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setActive(i == activeFieldIndex);
        }
    }

    private void createWorld(boolean playAfterCreate) {
        try {
            int width = clamp(parseInt(widthField.getValue(), 1024), 64, 8192);
            int height = clamp(parseInt(heightField.getValue(), 1024), 64, 8192);
            long seed = parseLong(seedField.getValue(), 0L);
            LocalWorldProfile profile = repository.createWorld(worldNameField.getValue(), width, height, seed);
            if (playAfterCreate) {
                repository.selectForLocalPlay(profile);
                ClientConfig config = repository.buildLocalClientConfig(ClientGame.newClientId());
                game.startGame(config);
            } else {
                messageColor = Color.LIGHT_GRAY;
                message = "World created: " + profile.getWorldDirectory();
            }
        } catch (Exception e) {
            messageColor = new Color(1f, 0.45f, 0.45f, 1f);
            message = "Could not create local world: " + e.getMessage();
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Long.parseLong(value.trim());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void render(float delta) {
        clearBackground();
        drawTitle("Create Local World", "Creates a dedicated save folder and world-settings.json under ~/.clientgame/worlds.");
        drawPanel(centerX(650f), 55f, 650f, 465f);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (TextInputField field : fields) {
            field.drawShape(shapes);
        }
        shapes.end();

        batch.begin();
        font.getData().setScale(0.95f);
        for (TextInputField field : fields) {
            field.drawText(batch, font);
        }
        font.setColor(new Color(0.72f, 0.80f, 0.90f, 1f));
        font.draw(batch, "Recommended now: 1024x1024. 2048+ can be heavy with full-map transfer.", centerX(600f), 155f);
        font.draw(batch, "The selected world is copied to the default local server room so :server:run can load it.", centerX(600f), 135f);
        batch.end();

        drawButtons();
        drawMessage(80f, 50f);
    }
}
