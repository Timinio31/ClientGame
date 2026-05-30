package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.client.ClientGame;
import com.tim.game.client.config.SavedServerRepository;
import com.tim.game.client.config.ServerProfile;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.ui.MenuButton;
import com.tim.game.client.ui.TextInputField;

import java.util.ArrayList;
import java.util.List;

public class DirectConnectScreen extends AbstractMenuScreen {
    private final SavedServerRepository repository = new SavedServerRepository();
    private final List<TextInputField> fields = new ArrayList<>();
    private int activeFieldIndex = 0;

    private TextInputField nameField;
    private TextInputField hostField;
    private TextInputField portField;
    private TextInputField roomField;
    private TextInputField rabbitUserField;
    private TextInputField rabbitPasswordField;
    private TextInputField virtualHostField;
    private TextInputField roomPasswordField;

    public DirectConnectScreen(ClientGame game) {
        super(game);
    }

    public DirectConnectScreen(ClientGame game, ServerProfile profile) {
        super(game);
        createFields(profile == null ? ServerProfile.localDefault() : profile.normalizedCopy());
    }

    @Override
    public void show() {
        super.show();
        if (fields.isEmpty()) {
            createFields(ServerProfile.localDefault());
        }

        float buttonY = 70f;
        buttons.clear();
        buttons.add(new MenuButton(80f, buttonY, 220f, 48f, "Connect", () -> connect(false)));
        buttons.add(new MenuButton(320f, buttonY, 260f, 48f, "Save + Connect", () -> connect(true)));
        buttons.add(new MenuButton(600f, buttonY, 220f, 48f, "Back", () -> game.setScreen(new MultiplayerMenuScreen(game))));

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
                if (keycode == Input.Keys.ENTER) {
                    connect(false);
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

    private void createFields(ServerProfile profile) {
        fields.clear();
        float leftX = 80f;
        float rightX = 510f;
        float startY = 430f;
        float gapY = 76f;
        float width = 350f;
        float height = 38f;

        nameField = new TextInputField("Profile name", profile.getName(), leftX, startY, width, height);
        hostField = new TextInputField("Host / IP", profile.getHost(), leftX, startY - gapY, width, height);
        portField = new TextInputField("RabbitMQ port", String.valueOf(profile.getPort()), leftX, startY - gapY * 2, width, height, false, true);
        roomField = new TextInputField("Room ID", profile.getRoomId(), leftX, startY - gapY * 3, width, height);

        rabbitUserField = new TextInputField("RabbitMQ user", profile.getUsername(), rightX, startY, width, height);
        rabbitPasswordField = new TextInputField("RabbitMQ password", profile.getPassword(), rightX, startY - gapY, width, height, true, false);
        virtualHostField = new TextInputField("Virtual host", profile.getVirtualHost(), rightX, startY - gapY * 2, width, height);
        roomPasswordField = new TextInputField("Room password (prepared)", profile.getRoomPassword(), rightX, startY - gapY * 3, width, height, true, false);

        fields.add(nameField);
        fields.add(hostField);
        fields.add(portField);
        fields.add(roomField);
        fields.add(rabbitUserField);
        fields.add(rabbitPasswordField);
        fields.add(virtualHostField);
        fields.add(roomPasswordField);
    }

    private void setActiveField(int index) {
        activeFieldIndex = index;
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setActive(i == activeFieldIndex);
        }
    }

    private void connect(boolean saveFirst) {
        try {
            ServerProfile profile = buildProfile();
            if (saveFirst) {
                repository.upsert(profile);
            }
            ClientConfig config = ClientConfig.fromServerProfile(profile, ClientGame.newClientId());
            game.startGame(config);
        } catch (Exception e) {
            messageColor = new Color(1f, 0.45f, 0.45f, 1f);
            message = "Invalid connection data: " + e.getMessage();
        }
    }

    private ServerProfile buildProfile() {
        int port = parsePort(portField.getValue());
        return new ServerProfile(
                nameField.getValue(),
                hostField.getValue(),
                port,
                rabbitUserField.getValue(),
                rabbitPasswordField.getValue(),
                virtualHostField.getValue(),
                roomField.getValue(),
                roomPasswordField.getValue()
        ).normalizedCopy();
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
        drawTitle("Direct Connect", "Enter RabbitMQ host/IP, room and optional room password. Tab switches fields, Enter connects.");
        drawPanel(50f, 50f, Gdx.graphics.getWidth() - 100f, Gdx.graphics.getHeight() - 165f);

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
        drawHintText(batch, font);
        batch.end();

        drawButtons();
        drawMessage(80f, 145f);
    }

    private void drawHintText(SpriteBatch batch, BitmapFont font) {
        font.setColor(new Color(0.78f, 0.82f, 0.90f, 1f));
        font.draw(batch,
                "Hinweis: Das Room-Passwort ist clientseitig vorbereitet. Serverseitige Prüfung kommt mit JOIN_ROOM/JOIN_ACCEPTED.",
                80f,
                150f);
    }
}
