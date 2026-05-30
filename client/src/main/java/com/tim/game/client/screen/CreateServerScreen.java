package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.client.ClientGame;
import com.tim.game.client.config.SavedServerRepository;
import com.tim.game.client.config.ServerProfile;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.ui.MenuButton;
import com.tim.game.client.ui.TextInputField;

import java.util.ArrayList;
import java.util.List;

public class CreateServerScreen extends AbstractMenuScreen {
    private final SavedServerRepository repository = new SavedServerRepository();
    private final List<TextInputField> fields = new ArrayList<>();
    private int activeFieldIndex = 0;

    private TextInputField serverNameField;
    private TextInputField hostField;
    private TextInputField portField;
    private TextInputField roomField;
    private TextInputField roomPasswordField;

    public CreateServerScreen(ClientGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();
        createFields();

        buttons.clear();
        buttons.add(new MenuButton(80f, 85f, 230f, 48f, "Save Profile", this::saveProfile));
        buttons.add(new MenuButton(330f, 85f, 260f, 48f, "Save + Connect", () -> {
            try {
                ServerProfile profile = saveProfileInternal();
                game.startGame(ClientConfig.fromServerProfile(profile, ClientGame.newClientId()));
            } catch (Exception e) {
                messageColor = new Color(1f, 0.45f, 0.45f, 1f);
                message = "Could not create profile: " + e.getMessage();
            }
        }));
        buttons.add(new MenuButton(610f, 85f, 220f, 48f, "Back", () -> game.setScreen(new MultiplayerMenuScreen(game))));

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
                    game.setScreen(new MultiplayerMenuScreen(game));
                    return true;
                }
                return false;
            }
        });
    }

    private void createFields() {
        fields.clear();
        float x = 100f;
        float y = 420f;
        float gap = 74f;
        float width = 420f;
        float height = 38f;

        serverNameField = new TextInputField("Server/profile name", "My Local Server", x, y, width, height);
        hostField = new TextInputField("Host/IP", "localhost", x, y - gap, width, height);
        portField = new TextInputField("RabbitMQ port", "5672", x, y - gap * 2, width, height, false, true);
        roomField = new TextInputField("Room ID", "1", x, y - gap * 3, width, height);
        roomPasswordField = new TextInputField("Room password (prepared)", "", x, y - gap * 4, width, height, true, false);

        fields.add(serverNameField);
        fields.add(hostField);
        fields.add(portField);
        fields.add(roomField);
        fields.add(roomPasswordField);
    }

    private void setActiveField(int index) {
        activeFieldIndex = index;
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setActive(i == activeFieldIndex);
        }
    }

    private void saveProfile() {
        try {
            saveProfileInternal();
            messageColor = Color.LIGHT_GRAY;
            message = "Server profile saved. Start RabbitMQ + the server application separately, then connect.";
        } catch (Exception e) {
            messageColor = new Color(1f, 0.45f, 0.45f, 1f);
            message = "Could not save profile: " + e.getMessage();
        }
    }

    private ServerProfile saveProfileInternal() {
        ServerProfile profile = new ServerProfile(
                serverNameField.getValue(),
                hostField.getValue(),
                parsePort(portField.getValue()),
                "guest",
                "guest",
                "/",
                roomField.getValue(),
                roomPasswordField.getValue()
        ).normalizedCopy();
        repository.upsert(profile);
        return profile;
    }

    private int parsePort(String value) {
        if (value == null || value.isBlank()) {
            return 5672;
        }
        int parsed = Integer.parseInt(value.trim());
        if (parsed <= 0 || parsed > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        return parsed;
    }

    @Override
    public void render(float delta) {
        clearBackground();
        drawTitle("Create Server Profile", "Creates a saved connection profile. Actual server process still starts via :server:run.");
        drawPanel(60f, 60f, Gdx.graphics.getWidth() - 120f, Gdx.graphics.getHeight() - 175f);

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
        font.setColor(new Color(0.78f, 0.82f, 0.90f, 1f));
        font.draw(batch, "Diese Funktion legt aktuell ein Serverprofil an. Ein echter Server-Host/Lobby-Dienst kommt später.", 570f, 430f);
        font.draw(batch, "Für lokale Tests: Docker RabbitMQ starten, :server:run starten, dann Save + Connect.", 570f, 400f);
        batch.end();

        drawButtons();
        drawMessage(80f, 160f);
    }
}
