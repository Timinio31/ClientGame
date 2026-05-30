package com.tim.game.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.net.ClientMessageBus;
import com.tim.game.client.net.ClientRabbitConnection;
import com.tim.game.client.net.SnapshotBuffer;
import com.tim.game.client.render.WorldRenderer;
import com.tim.game.shared.DTOs.input.BuildInputDto;
import com.tim.game.shared.DTOs.input.MoveInputDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.MessageType;
import com.tim.game.shared.model.Vector2f;

import java.util.concurrent.atomic.AtomicReference;

public class ClientGame extends ApplicationAdapter {

    private static final float TILE_SIZE = 1.0f;
    private static final float ZOOM_MIN = 0.5f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float ZOOM_STEP = 0.1f;
    private static final float MOVE_REPEAT_INTERVAL = 0.15f;

    private OrthographicCamera camera;
    private ShapeRenderer shapes;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ClientConfig cfg;
    private ClientRabbitConnection rabbit;
    private ClientMessageBus bus;
    private SnapshotBuffer snapshotBuffer;

    private final AtomicReference<MapInitDto> lastMapInit = new AtomicReference<>();
    private final AtomicReference<WorldSnapshotDto> lastSnapshot = new AtomicReference<>();

    private float scrollY = 0f;
    private float moveRepeatTimer = 0f;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 32, 18);
        shapes = new ShapeRenderer();

        String clientId = "c" + System.currentTimeMillis();
        cfg = ClientConfig.localDefault(clientId);

        snapshotBuffer = new SnapshotBuffer();
        rabbit = new ClientRabbitConnection();
        bus = new ClientMessageBus();
        setInputProcessor();

        try {
            rabbit.connect(cfg);
            bus.init(rabbit.channel(), cfg);
            bus.startConsumingUpdates();
            sendInitialPing();
        } catch (Exception e) {
            throw new RuntimeException("RabbitMQ connect failed", e);
        }
    }

    private void setInputProcessor() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                scrollY += amountY;
                return true;
            }
        });
    }

    private void sendInitialPing() {
        try {
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.PING,
                    cfg.roomId,
                    cfg.clientId,
                    "{}"
            );
            bus.publishCommand(cfg, commandMessage);
            System.out.println("[Client] Sent initial PING for MAP_INIT request");
        } catch (Exception e) {
            System.err.println("[Client] Initial PING publish failed:");
            e.printStackTrace();
        }
    }

    @Override
    public void render() {
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
        WorldRenderer.renderWorld(camera, shapes, mapInit, snapshot, cfg.clientId);
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

        BuildInputDto dto = new BuildInputDto("GENERATOR", tileX, tileY);

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

    private void handleZoom() {
        if (scrollY != 0f) {
            camera.zoom = MathUtils.clamp(camera.zoom + scrollY * ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
            scrollY = 0f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            camera.zoom = 1.0f;
        }
    }

    @SuppressWarnings("unused")
    private void renderDebugCross(ShapeRenderer shapes) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.WHITE);
        shapes.rect(-0.05f, -1f, 0.1f, 2f);
        shapes.rect(-1f, -0.05f, 2f, 0.1f);
        shapes.end();
    }

    @Override
    public void dispose() {
        if (shapes != null) {
            shapes.dispose();
        }
        if (rabbit != null) {
            rabbit.close();
        }
    }
}
