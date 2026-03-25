package com.tim.game.client;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.game.client.net.*;
import com.tim.game.shared.DTOs.input.MoveInputDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.MessageType;
import com.tim.game.shared.model.Vector2f;
import java.util.concurrent.atomic.AtomicReference;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.MathUtils;

import com.badlogic.gdx.math.Vector3;
import com.tim.game.shared.DTOs.input.BuildInputDto;
import com.tim.game.shared.DTOs.update.BuildingStateDto;

import com.tim.game.client.render.WorldRenderer;


public class ClientGame extends ApplicationAdapter {
    private OrthographicCamera camera;
    private ShapeRenderer shapes;

    private final ObjectMapper mapper = new ObjectMapper();

    private ClientConfig cfg;
    private ClientRabbitConnection rabbit;
    private ClientMessageBus bus;
    private SnapshotBuffer snapshotBuffer;
    private final AtomicReference<WorldSnapshotDto> lastSnapshot = new AtomicReference<>();

    private float scrollY = 0f;
    private static final float ZOOM_MIN = 0.5f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float ZOOM_STEP = 0.1f;

    private static final float TILE_SIZE = 1.0f;

    private static final float MOVE_REPEAT_INTERVAL = 0.15f; // 150ms pro Tile
    private float moveRepeatTimer = 0f;



    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 32, 18); // World units
        shapes = new ShapeRenderer();

        // simple clientId (später: Login/UUID)
        String clientId = "c" + System.currentTimeMillis();
        cfg = ClientConfig.localDefault(clientId);

        snapshotBuffer = new SnapshotBuffer();
        rabbit = new ClientRabbitConnection();
        bus = new ClientMessageBus();
        setInputProccesor();

        try {
            rabbit.connect(cfg);
            bus.init(rabbit.channel(), cfg);
            bus.startConsumingUpdates();
        } catch (Exception e) {
            throw new RuntimeException("RabbitMQ connect failed", e);
        }
    }

    public void setInputProccesor(){
         Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                // amountY: meist +1/-1 pro Scroll-Raster (Trackpad kann feinere Werte liefern)
                scrollY += amountY;
                return true;
            }
        });
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
    public void render() {
        // 1) Consume updates
        EventMessage ev;
        while ((ev = bus.pollEvent()) != null) {
            if (ev.getType() == MessageType.WORLD_SNAPSHOT) {
                try {
                    WorldSnapshotDto snap = mapper.readValue(ev.getPayloadJson(), WorldSnapshotDto.class);
                    lastSnapshot.set(snap);
                } catch (Exception e) {
                    System.err.println("[Client] Failed to parse snapshot:");
                    e.printStackTrace();
                }
            }
        }

        // 2) Input -> send MOVE
        handleInput();
        handleBuildInput();
        handleZoom();

        // 3) Bildschirm löschen
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 4) Kamera auf lokalen Spieler zentrieren
        WorldSnapshotDto snapshot = lastSnapshot.get();
        Vector2f localPlayerPos = WorldRenderer.getLocalPlayerPos(snapshot, cfg.clientId);

        if (localPlayerPos != null) {
            camera.position.set(localPlayerPos.getX(), localPlayerPos.getY(), 0f);
        } else {
            camera.position.set(16f, 16f, 0f);
        }

        camera.update();

        // 5) Welt rendern
        WorldRenderer.renderWorld(camera, shapes, snapshot, cfg.clientId);
    }


    private void handleInput() {

        moveRepeatTimer += Gdx.graphics.getDeltaTime();
        if (moveRepeatTimer < MOVE_REPEAT_INTERVAL) {
            return;
        }

        int directionX = 0;
        int directionY = 0;

        // Halten = weiterlaufen (keine Diagonalen, Priorität vertikal)
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            directionY = 1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            directionY = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            directionX = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            directionX = 1;
        } else {
            return; // nichts gehalten
        }

        // Step wurde ausgelöst -> Timer reset
        moveRepeatTimer = 0f;

        MoveInputDto moveInput = new MoveInputDto(new Vector2f(directionX, directionY));

        try {
            String payloadJson = mapper.writeValueAsString(moveInput);

            CommandMessage commandMessage = new CommandMessage();
            commandMessage.setType(MessageType.MOVE);
            commandMessage.setRoomId(cfg.roomId);
            commandMessage.setClientId(cfg.clientId);
            commandMessage.setPayloadJson(payloadJson);

            bus.publishCommand(cfg, commandMessage);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    private void renderDebugCross(ShapeRenderer shapes) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.WHITE);
        shapes.rect(-0.05f, -1f, 0.1f, 2f);
        shapes.rect(-1f, -0.05f, 2f, 0.1f);
        shapes.end();
    }




    @Override
    public void dispose() {
        shapes.dispose();
        rabbit.close();
    }
    

    private void handleBuildInput() {
        // Rechtsklick = bauen (einfacher Start)
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) return;

        Vector3 world = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        camera.unproject(world);

        int tileX = (int)Math.floor(world.x / TILE_SIZE);
        int tileY = (int)Math.floor(world.y / TILE_SIZE);

        BuildInputDto dto = new BuildInputDto("GENERATOR", tileX, tileY);

        try {
            String payload = mapper.writeValueAsString(dto);
            CommandMessage commandMessage = new CommandMessage();
            commandMessage.setType(MessageType.BUILD);
            commandMessage.setRoomId(cfg.roomId);
            commandMessage.setClientId(cfg.clientId);
            commandMessage.setPayloadJson(payload);

            bus.publishCommand(cfg, commandMessage);
            System.out.println("[Client] Sent BUILD at tile " + tileX + "," + tileY);
        } catch (Exception e) {
            System.err.println("[Client] BUILD publish failed:");
            e.printStackTrace();
        }
    }

}
