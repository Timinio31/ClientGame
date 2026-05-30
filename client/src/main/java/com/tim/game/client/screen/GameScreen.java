package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.game.client.ClientGame;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.net.ClientMessageBus;
import com.tim.game.client.net.ClientRabbitConnection;
import com.tim.game.client.net.SnapshotBuffer;
import com.tim.game.client.render.WorldRenderer;
import com.tim.game.client.ui.InventoryHudRenderer;
import com.tim.game.shared.DTOs.input.BuildInputDto;
import com.tim.game.shared.DTOs.input.InventoryActionInputDto;
import com.tim.game.shared.DTOs.input.MoveInputDto;
import com.tim.game.shared.DTOs.update.InventorySlotDto;
import com.tim.game.shared.DTOs.update.ItemStackDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.MessageType;
import com.tim.game.shared.model.Vector2f;

import java.util.concurrent.atomic.AtomicReference;

public class GameScreen extends ScreenAdapter {

    private static final float TILE_SIZE = 1.0f;
    private static final float ZOOM_MIN = 0.5f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float ZOOM_STEP = 0.1f;
    private static final float MOVE_REPEAT_INTERVAL = 0.15f;

    private final ClientGame game;
    private final ClientConfig cfg;

    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private InventoryHudRenderer inventoryHudRenderer;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ClientRabbitConnection rabbit;
    private ClientMessageBus bus;
    private SnapshotBuffer snapshotBuffer;

    private final AtomicReference<MapInitDto> lastMapInit = new AtomicReference<>();
    private final AtomicReference<WorldSnapshotDto> lastSnapshot = new AtomicReference<>();

    private float scrollY = 0f;
    private float moveRepeatTimer = 0f;
    private boolean connectionFailed;
    private boolean inventoryExpanded;
    private String connectionError = "";

    public GameScreen(ClientGame game, ClientConfig cfg) {
        this.game = game;
        this.cfg = cfg;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 32, 18);
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        inventoryHudRenderer = new InventoryHudRenderer();
        font.getData().setScale(1.1f);

        snapshotBuffer = new SnapshotBuffer();
        rabbit = new ClientRabbitConnection();
        bus = new ClientMessageBus();
        setInputProcessor();

        try {
            rabbit.connect(cfg);
            bus.init(rabbit.channel(), cfg);
            bus.startConsumingUpdates();
            sendPingForMapInit();
        } catch (Exception e) {
            connectionFailed = true;
            connectionError = e.getMessage() == null ? e.toString() : e.getMessage();
            System.err.println("[Client] RabbitMQ connect failed: " + connectionError);
            e.printStackTrace();
        }
    }

    private void setInputProcessor() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                scrollY += amountY;
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    leaveToMenu();
                    return true;
                }
                if (keycode == Input.Keys.F5) {
                    game.getTexturePackManager().reloadActivePack();
                    return true;
                }
                if (keycode == Input.Keys.I) {
                    inventoryExpanded = !inventoryExpanded;
                    return true;
                }
                if (keycode == Input.Keys.E) {
                    sendInventoryAction("PICKUP_NEAREST", -1, 1);
                    return true;
                }
                if (keycode == Input.Keys.Q) {
                    sendInventoryAction("DROP_SELECTED", -1, 1);
                    return true;
                }
                if (keycode == Input.Keys.F) {
                    sendInventoryAction("USE_SELECTED", -1, 1);
                    return true;
                }

                int selectedSlot = hotbarSlotForKey(keycode);
                if (selectedSlot >= 0) {
                    sendInventoryAction("SELECT_SLOT", selectedSlot, 1);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        if (connectionFailed) {
            renderConnectionError();
            return;
        }

        consumeServerEvents();

        handleInput();
        handleBuildInput();
        handleZoom();

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        MapInitDto mapInit = lastMapInit.get();
        WorldSnapshotDto snapshot = lastSnapshot.get();
        Vector2f localPlayerPos = WorldRenderer.getLocalPlayerPos(snapshot, cfg.clientId);

        if (localPlayerPos != null) {
            camera.position.set(localPlayerPos.getX(), localPlayerPos.getY(), 0f);
        } else if (mapInit != null) {
            camera.position.set(mapInit.getWidth() * mapInit.getTileSize() * 0.5f,
                    mapInit.getHeight() * mapInit.getTileSize() * 0.5f,
                    0f);
        } else {
            camera.position.set(16f, 16f, 0f);
        }

        camera.update();
        WorldRenderer.renderWorld(camera, shapes, batch, mapInit, snapshot, cfg.clientId);
        renderOverlay();
    }

    private void renderConnectionError() {
        Gdx.gl.glClearColor(0.055f, 0.06f, 0.085f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.getData().setScale(1.45f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Connection failed", 60f, Gdx.graphics.getHeight() - 70f);
        font.getData().setScale(1.0f);
        font.setColor(new Color(1f, 0.45f, 0.45f, 1f));
        font.draw(batch, connectionError, 60f, Gdx.graphics.getHeight() - 115f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Check host/IP, port, RabbitMQ credentials and whether the server is running.", 60f, Gdx.graphics.getHeight() - 150f);
        font.draw(batch, "Press ESC to return to the main menu.", 60f, Gdx.graphics.getHeight() - 185f);
        batch.end();
    }

    private void renderOverlay() {
        updateHudCamera();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(0.85f);
        font.setColor(Color.LIGHT_GRAY);
        String activePack = game.getTexturePackManager().getActivePack() == null
                ? "none"
                : game.getTexturePackManager().getActivePack().getDisplayName();
        String info = "ESC: Menu | F5: Textures | WASD: Move | 1-8: Slot | I: Inventory | E: Pickup | F: Use | Q: Drop | Right click: Build"
                + " | Host " + cfg.rabbitHost + ":" + cfg.rabbitPort + " | Room " + cfg.roomId + " | Pack " + activePack;
        font.draw(batch, info, 12f, Gdx.graphics.getHeight() - 12f);
        if (lastMapInit.get() == null) {
            font.setColor(new Color(1f, 0.86f, 0.35f, 1f));
            font.draw(batch, "Waiting for MAP_INIT. Move once if the server did not receive the initial PING.", 12f, Gdx.graphics.getHeight() - 36f);
        }
        batch.end();

        inventoryHudRenderer.render(shapes, batch, font, hudCamera, lastSnapshot.get(), cfg.clientId, inventoryExpanded);
    }

    private void consumeServerEvents() {
        EventMessage event;
        while ((event = bus.pollEvent()) != null) {
            try {
                if (event.getType() == MessageType.MAP_INIT) {
                    MapInitDto mapInit = mapper.readValue(event.getPayloadJson(), MapInitDto.class);
                    lastMapInit.set(mapInit);
                    System.out.println("[Client] MAP_INIT received: room=" + mapInit.getRoomId()
                            + " size=" + mapInit.getWidth() + "x" + mapInit.getHeight()
                            + " tiles=" + mapInit.getTiles().size());
                } else if (event.getType() == MessageType.WORLD_SNAPSHOT) {
                    WorldSnapshotDto snapshot = mapper.readValue(event.getPayloadJson(), WorldSnapshotDto.class);
                    lastSnapshot.set(snapshot);
                    snapshotBuffer.set(snapshot);
                }
            } catch (Exception e) {
                System.err.println("[Client] Failed to parse event type=" + event.getType());
                e.printStackTrace();
            }
        }
    }

    private void sendPingForMapInit() {
        try {
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.PING,
                    cfg.roomId,
                    cfg.clientId,
                    "{}"
            );
            bus.publishCommand(cfg, commandMessage);
        } catch (Exception exception) {
            System.err.println("[Client] Initial PING failed:");
            exception.printStackTrace();
        }
    }

    private void sendInventoryAction(String action, int slotIndex, int amount) {
        try {
            InventoryActionInputDto input = new InventoryActionInputDto(action, slotIndex, amount, -1, -1);
            String payloadJson = mapper.writeValueAsString(input);
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.INVENTORY,
                    cfg.roomId,
                    cfg.clientId,
                    payloadJson
            );
            bus.publishCommand(cfg, commandMessage);
        } catch (Exception exception) {
            System.err.println("[Client] INVENTORY publish failed:");
            exception.printStackTrace();
        }
    }

    private int hotbarSlotForKey(int keycode) {
        return switch (keycode) {
            case Input.Keys.NUM_1, Input.Keys.NUMPAD_1 -> 0;
            case Input.Keys.NUM_2, Input.Keys.NUMPAD_2 -> 1;
            case Input.Keys.NUM_3, Input.Keys.NUMPAD_3 -> 2;
            case Input.Keys.NUM_4, Input.Keys.NUMPAD_4 -> 3;
            case Input.Keys.NUM_5, Input.Keys.NUMPAD_5 -> 4;
            case Input.Keys.NUM_6, Input.Keys.NUMPAD_6 -> 5;
            case Input.Keys.NUM_7, Input.Keys.NUMPAD_7 -> 6;
            case Input.Keys.NUM_8, Input.Keys.NUMPAD_8 -> 7;
            default -> -1;
        };
    }

    private void handleInput() {
        moveRepeatTimer += Gdx.graphics.getDeltaTime();
        if (moveRepeatTimer < MOVE_REPEAT_INTERVAL) {
            return;
        }

        int directionX = 0;
        int directionY = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            directionY = 1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            directionY = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            directionX = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            directionX = 1;
        } else {
            return;
        }

        moveRepeatTimer = 0f;
        MoveInputDto moveInput = new MoveInputDto(new Vector2f(directionX, directionY));

        try {
            String payloadJson = mapper.writeValueAsString(moveInput);
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.MOVE,
                    cfg.roomId,
                    cfg.clientId,
                    payloadJson
            );

            bus.publishCommand(cfg, commandMessage);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void handleBuildInput() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            return;
        }

        Vector3 world = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        camera.unproject(world);

        int tileX = (int) Math.floor(world.x / TILE_SIZE);
        int tileY = (int) Math.floor(world.y / TILE_SIZE);

        String buildingType = getSelectedBuildType(lastSnapshot.get());
        if (buildingType == null) {
            return;
        }

        BuildInputDto dto = new BuildInputDto(buildingType, tileX, tileY);

        try {
            String payload = mapper.writeValueAsString(dto);
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.BUILD,
                    cfg.roomId,
                    cfg.clientId,
                    payload
            );

            bus.publishCommand(cfg, commandMessage);
            System.out.println("[Client] Sent BUILD at tile " + tileX + "," + tileY);
        } catch (Exception e) {
            System.err.println("[Client] BUILD publish failed:");
            e.printStackTrace();
        }
    }

    private String getSelectedBuildType(WorldSnapshotDto snapshot) {
        PlayerStateDto localPlayer = findLocalPlayer(snapshot);
        if (localPlayer == null || localPlayer.getInventorySlots() == null) {
            return null;
        }

        for (InventorySlotDto slot : localPlayer.getInventorySlots()) {
            if (slot == null || slot.getSlotIndex() != localPlayer.getSelectedInventorySlot()) {
                continue;
            }
            ItemStackDto item = slot.getItem();
            if (item != null && !item.isEmpty() && item.isPlaceable() && item.getBuildType() != null && !item.getBuildType().isBlank()) {
                return item.getBuildType();
            }
        }

        return null;
    }

    private PlayerStateDto findLocalPlayer(WorldSnapshotDto snapshot) {
        if (snapshot == null || snapshot.getPlayers() == null) {
            return null;
        }

        for (PlayerStateDto player : snapshot.getPlayers()) {
            if (player == null) {
                continue;
            }
            if (cfg.clientId.equals(player.getClientId())) {
                return player;
            }
        }

        return null;
    }

    private void handleZoom() {
        if (scrollY != 0f) {
            camera.zoom = MathUtils.clamp(camera.zoom + scrollY * ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
            scrollY = 0f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            camera.zoom = 1.0f;
        }
    }

    @Override
    public void resize(int width, int height) {
        updateHudCamera();
    }

    private void updateHudCamera() {
        if (hudCamera == null) {
            return;
        }
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.update();
    }

    private void leaveToMenu() {
        dispose();
        game.showMainMenu();
    }

    @Override
    public void dispose() {
        if (shapes != null) {
            shapes.dispose();
            shapes = null;
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (rabbit != null) {
            rabbit.close();
            rabbit = null;
        }
    }
}
