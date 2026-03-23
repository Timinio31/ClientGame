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

        // 3) Render latest snapshot
        renderWorld(lastSnapshot.get());
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



    private Vector2f getLocalPlayerPos(WorldSnapshotDto snap) {
        if (snap == null || snap.getPlayers() == null) return null;

        for (PlayerStateDto player : snap.getPlayers()) {
            if (!cfg.clientId.equals(player.getClientId())) continue;
            if (player.getPosition() == null) return null;
            return player.getPosition();
        }
        return null;
    }

    private void renderPlayers(ShapeRenderer shapes, WorldSnapshotDto snap) {
        if (snap == null || snap.getPlayers() == null) return;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.LIME);

        for (PlayerStateDto player : snap.getPlayers()) {
            if (player.getPosition() == null) continue;
            float playerPostionX = player.getPosition().getX();
            float playerPostionY = player.getPosition().getY();
            shapes.circle(playerPostionX, playerPostionY, 0.6f, 24);
        }

        shapes.end();
    }

    private void renderBuildings(ShapeRenderer shapes, WorldSnapshotDto snap, float tileSize) {
        if (snap == null || snap.getBuildings() == null) return;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.ORANGE);

        for (BuildingStateDto building : snap.getBuildings()) {
            if (building.getPosition() == null) continue;

            float cx = building.getPosition().getX();
            float cy = building.getPosition().getY();
            shapes.rect(cx - tileSize * 0.5f, cy - tileSize * 0.5f, tileSize, tileSize);
        }

        shapes.end();
    }


    private void renderGrid(OrthographicCamera camera, ShapeRenderer shapes, float tileSize) {
        // Sichtbereich (approx)
        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        float left   = camera.position.x - halfW;
        float right  = camera.position.x + halfW;
        float bottom = camera.position.y - halfH;
        float top    = camera.position.y + halfH;

        int startX = (int)Math.floor(left / tileSize) - 1;
        int endX   = (int)Math.floor(right / tileSize) + 1;
        int startY = (int)Math.floor(bottom / tileSize) - 1;
        int endY   = (int)Math.floor(top / tileSize) + 1;

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.DARK_GRAY);

        // vertikale Linien
        for (int x = startX; x <= endX; x++) {
            float wx = x * tileSize;
            shapes.line(wx, startY * tileSize, wx, endY * tileSize);
        }

        // horizontale Linien
        for (int y = startY; y <= endY; y++) {
            float wy = y * tileSize;
            shapes.line(startX * tileSize, wy, endX * tileSize, wy);
        }

        shapes.end();
    }


    private void renderWorld(WorldSnapshotDto snap) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Kamera auf lokalen Spieler
        Vector2f myPos = getLocalPlayerPos(snap);
        float camX = (myPos != null) ? myPos.getX() : 0f;
        float camY = (myPos != null) ? myPos.getY() : 0f;

        camera.position.set(camX, camY, 0f);
        camera.update();

        shapes.setProjectionMatrix(camera.combined);

        // 1) Grid
        renderGrid(camera, shapes, TILE_SIZE);

        // 2) Buildings
        renderBuildings(shapes, snap, TILE_SIZE);

        // 3) Debug cross (optional)
        renderDebugCross(shapes);

        // 4) Players
        renderPlayers(shapes, snap);
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
