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
import com.tim.game.client.ui.CraftingMenuRenderer;
import com.tim.game.client.ui.BibbleMenuRenderer;
import com.tim.game.client.ui.InventoryHudRenderer;
import com.tim.game.shared.DTOs.input.BuildInputDto;
import com.tim.game.shared.DTOs.input.ActionInputDto;
import com.tim.game.shared.DTOs.input.BibbleInputDto;
import com.tim.game.shared.DTOs.input.CraftingInputDto;
import com.tim.game.shared.DTOs.input.InventoryActionInputDto;
import com.tim.game.shared.DTOs.input.MoveInputDto;
import com.tim.game.shared.DTOs.input.MapChunkRequestInputDto;
import com.tim.game.shared.DTOs.update.InventorySlotDto;
import com.tim.game.shared.DTOs.update.ItemStackDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.MapChunkDto;
import com.tim.game.shared.DTOs.update.TileStateDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.config.WorldSettings;
import com.tim.game.shared.crafting.CraftingRecipe;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.MessageType;
import com.tim.game.shared.model.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class GameScreen extends ScreenAdapter {

    private static final float TILE_SIZE = 1.0f;
    private static final float ZOOM_MIN = 0.5f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float ZOOM_STEP = 0.1f;
    private static final float DEFAULT_MOVE_REPEAT_INTERVAL = 0.15f;

    private final ClientGame game;
    private final ClientConfig cfg;

    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private InventoryHudRenderer inventoryHudRenderer;
    private CraftingMenuRenderer craftingMenuRenderer;
    private BibbleMenuRenderer bibbleMenuRenderer;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ClientRabbitConnection rabbit;
    private ClientMessageBus bus;
    private SnapshotBuffer snapshotBuffer;

    private final AtomicReference<MapInitDto> lastMapInit = new AtomicReference<>();
    private final AtomicReference<WorldSnapshotDto> lastSnapshot = new AtomicReference<>();
    private final AtomicReference<WorldSettings> lastWorldSettings = new AtomicReference<>(WorldSettings.defaults());
    private static final int MAX_INITIAL_CHUNK_REQUESTS_PER_FRAME = 32;

    private final Map<String, TileStateDto> streamedTiles = new ConcurrentHashMap<>();
    private final Set<String> loadedChunks = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingChunks = ConcurrentHashMap.newKeySet();

    private float scrollY = 0f;
    private float moveRepeatTimer = 0f;
    private boolean connectionFailed;
    private boolean inventoryExpanded;
    private boolean craftingMenuOpen;
    private boolean bibbleMenuOpen;
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
        craftingMenuRenderer = new CraftingMenuRenderer();
        bibbleMenuRenderer = new BibbleMenuRenderer();
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
                    if (craftingMenuOpen) {
                        craftingMenuOpen = false;
                    } else if (bibbleMenuOpen) {
                        bibbleMenuOpen = false;
                    } else {
                        leaveToMenu();
                    }
                    return true;
                }
                if (keycode == Input.Keys.F5) {
                    game.getTexturePackManager().reloadActivePack();
                    return true;
                }
                if (keycode == Input.Keys.B) {
                    if (BibbleMenuRenderer.isBibbleMenuAllowed(currentSettings())) {
                        bibbleMenuOpen = !bibbleMenuOpen;
                        if (bibbleMenuOpen) {
                            craftingMenuOpen = false;
                        }
                    }
                    return true;
                }
                if (bibbleMenuOpen) {
                    return handleBibbleMenuKey(keycode);
                }
                if (keycode == Input.Keys.C) {
                    if (CraftingMenuRenderer.isCraftingMenuAllowed(currentSettings())) {
                        craftingMenuOpen = !craftingMenuOpen;
                        if (craftingMenuOpen) {
                            bibbleMenuOpen = false;
                        }
                    }
                    return true;
                }
                if (keycode == Input.Keys.I) {
                    if (currentSettings().isInventoryEnabled()) {
                        inventoryExpanded = !inventoryExpanded;
                    }
                    return true;
                }
                if (keycode == Input.Keys.E) {
                    if (currentSettings().isInventoryEnabled() && currentSettings().isWorldItemsEnabled()) {
                        sendInventoryAction("PICKUP_NEAREST", -1, 1);
                    }
                    return true;
                }
                if (keycode == Input.Keys.Q) {
                    if (currentSettings().isInventoryEnabled() && currentSettings().isWorldItemsEnabled()) {
                        sendInventoryAction("DROP_SELECTED", -1, 1);
                    }
                    return true;
                }
                if (keycode == Input.Keys.F) {
                    if (currentSettings().isInventoryEnabled()) {
                        sendInventoryAction("USE_SELECTED", -1, 1);
                    }
                    return true;
                }

                int selectedSlot = hotbarSlotForKey(keycode);
                if (selectedSlot >= 0) {
                    if (craftingMenuOpen) {
                        craftVisibleRecipe(selectedSlot);
                    } else if (currentSettings().isInventoryEnabled()) {
                        sendInventoryAction("SELECT_SLOT", selectedSlot, 1);
                    }
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

        MapInitDto mapInit = lastMapInit.get();
        preloadInitialMapChunks(mapInit);
        boolean mapPreloading = isInitialMapPreloading(mapInit);

        if (!mapPreloading) {
            handleInput();
            handleActionInput();
            handleBuildInput();
        }
        handleZoom();

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (mapPreloading) {
            renderInitialMapLoading(mapInit);
            return;
        }

        WorldSnapshotDto snapshot = lastSnapshot.get();
        Vector2f localPlayerPos = WorldRenderer.getLocalPlayerPos(snapshot, cfg.clientId);

        if (localPlayerPos != null) {
            camera.position.set(localPlayerPos.getX(), localPlayerPos.getY(), 0f);
        } else if (mapInit != null && mapInit.getSpawnPoints() != null && !mapInit.getSpawnPoints().isEmpty()) {
            Vector2f spawn = mapInit.getSpawnPoints().get(0);
            camera.position.set(spawn.getX(), spawn.getY(), 0f);
        } else if (mapInit != null) {
            camera.position.set(mapInit.getWidth() * mapInit.getTileSize() * 0.5f,
                    mapInit.getHeight() * mapInit.getTileSize() * 0.5f,
                    0f);
        } else {
            camera.position.set(16f, 16f, 0f);
        }

        camera.update();
        WorldRenderer.renderWorld(camera, shapes, batch, mapInit, snapshot, cfg.clientId, streamedTiles);
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
        WorldSettings settings = currentSettings();
        String info = "ESC: Menu | F5: Textures | WASD: Move | 1-8: Slot/Craft | I: Inventory | C: Crafting | B: Bibbles | E: Pickup | F: Use | Q: Drop | Right click: Build"
                + " | Host " + cfg.rabbitHost + ":" + cfg.rabbitPort + " | Room " + cfg.roomId + " | Pack " + activePack;
        font.draw(batch, info, 12f, Gdx.graphics.getHeight() - 12f);
        font.draw(batch, "World: " + settings.getWorldName() + " | Map " + settings.getMapId() + " (" + settings.getMapMode() + ")"
                + " | Features: inv=" + settings.isInventoryEnabled()
                + ", build=" + settings.isBuildingEnabled()
                + ", craft=" + settings.isCraftingEnabled()
                + ", bibbles=" + settings.isBibblesEnabled()
                + ", combat=" + settings.isCombatEnabled(), 12f, Gdx.graphics.getHeight() - 36f);
        if (lastMapInit.get() == null) {
            font.setColor(new Color(1f, 0.86f, 0.35f, 1f));
            font.draw(batch, "Waiting for MAP_INIT. Move once if the server did not receive the initial PING.", 12f, Gdx.graphics.getHeight() - 60f);
        }
        batch.end();

        inventoryHudRenderer.render(shapes, batch, font, hudCamera, lastSnapshot.get(), cfg.clientId, inventoryExpanded);
        craftingMenuRenderer.render(shapes, batch, font, hudCamera, lastSnapshot.get(), cfg.clientId, currentSettings(), craftingMenuOpen);
        bibbleMenuRenderer.render(shapes, batch, font, hudCamera, lastSnapshot.get(), cfg.clientId, currentSettings(), bibbleMenuOpen);
    }

    private void consumeServerEvents() {
        EventMessage event;
        while ((event = bus.pollEvent()) != null) {
            try {
                if (event.getType() == MessageType.MAP_INIT) {
                    MapInitDto mapInit = mapper.readValue(event.getPayloadJson(), MapInitDto.class);
                    lastMapInit.set(mapInit);
                    streamedTiles.clear();
                    loadedChunks.clear();
                    pendingChunks.clear();
                    boolean receivedFullTileList = mapInit.getTiles() != null && !mapInit.getTiles().isEmpty();
                    if (receivedFullTileList) {
                        indexTiles(mapInit.getTiles(), Math.max(1, mapInit.getChunkSize()));
                        if (streamedTiles.size() >= expectedTileCount(mapInit)) {
                            markAllChunksLoaded(mapInit);
                        }
                        // The renderer uses streamedTiles as a spatial lookup now. Keeping the full DTO
                        // list in mapInit would duplicate memory and make every frame iterate the full map.
                        mapInit.setTiles(new ArrayList<>());
                    }
                    WorldSettings receivedSettings = mapInit.getWorldSettings() == null ? WorldSettings.defaults() : mapInit.getWorldSettings().normalizedCopy();
                    lastWorldSettings.set(receivedSettings);
                    if (!CraftingMenuRenderer.isCraftingMenuAllowed(receivedSettings)) {
                        craftingMenuOpen = false;
                    }
                    if (!BibbleMenuRenderer.isBibbleMenuAllowed(receivedSettings)) {
                        bibbleMenuOpen = false;
                    }
                    System.out.println("[Client] MAP_INIT received: room=" + mapInit.getRoomId()
                            + " size=" + mapInit.getWidth() + "x" + mapInit.getHeight()
                            + " chunks=" + mapInit.getChunkSize()
                            + " streaming=" + mapInit.isChunkStreamingEnabled()
                            + " tiles=" + mapInit.getTiles().size());
                } else if (event.getType() == MessageType.MAP_CHUNK) {
                    MapChunkDto chunk = mapper.readValue(event.getPayloadJson(), MapChunkDto.class);
                    String chunkKey = chunkKey(chunk.getChunkX(), chunk.getChunkY());
                    pendingChunks.remove(chunkKey);
                    loadedChunks.add(chunkKey);
                    if (chunk.getTiles() != null) {
                        indexTiles(chunk.getTiles(), Math.max(1, chunk.getChunkSize()));
                    }
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

    private void preloadInitialMapChunks(MapInitDto mapInit) {
        if (mapInit == null || !mapInit.isChunkStreamingEnabled() || bus == null) {
            return;
        }

        int totalChunks = expectedChunkCount(mapInit);
        if (totalChunks <= 0 || loadedChunks.size() >= totalChunks) {
            pendingChunks.clear();
            return;
        }

        int chunkSize = Math.max(1, mapInit.getChunkSize());
        int lastChunkX = Math.max(0, (mapInit.getWidth() - 1) / chunkSize);
        int lastChunkY = Math.max(0, (mapInit.getHeight() - 1) / chunkSize);

        int requestedThisFrame = 0;
        for (int cy = 0; cy <= lastChunkY; cy++) {
            for (int cx = 0; cx <= lastChunkX; cx++) {
                if (requestedThisFrame >= MAX_INITIAL_CHUNK_REQUESTS_PER_FRAME) {
                    return;
                }
                if (requestChunk(mapInit, cx, cy)) {
                    requestedThisFrame++;
                }
            }
        }
    }

    private boolean isInitialMapPreloading(MapInitDto mapInit) {
        if (mapInit == null || !mapInit.isChunkStreamingEnabled()) {
            return false;
        }
        int totalChunks = expectedChunkCount(mapInit);
        return totalChunks > 0 && loadedChunks.size() < totalChunks;
    }

    private int expectedChunkCount(MapInitDto mapInit) {
        if (mapInit == null) {
            return 0;
        }
        int chunkSize = Math.max(1, mapInit.getChunkSize());
        int chunksX = Math.max(1, ((mapInit.getWidth() - 1) / chunkSize) + 1);
        int chunksY = Math.max(1, ((mapInit.getHeight() - 1) / chunkSize) + 1);
        return chunksX * chunksY;
    }

    private int expectedTileCount(MapInitDto mapInit) {
        if (mapInit == null) {
            return 0;
        }
        return Math.max(0, mapInit.getWidth()) * Math.max(0, mapInit.getHeight());
    }

    private void indexTiles(List<TileStateDto> tiles, int chunkSize) {
        if (tiles == null) {
            return;
        }
        int safeChunkSize = Math.max(1, chunkSize);
        for (TileStateDto tile : tiles) {
            if (tile == null) {
                continue;
            }
            streamedTiles.put(tileKey(tile.getX(), tile.getY()), tile);
            loadedChunks.add(chunkKey(Math.floorDiv(tile.getX(), safeChunkSize), Math.floorDiv(tile.getY(), safeChunkSize)));
        }
    }

    private void markAllChunksLoaded(MapInitDto mapInit) {
        if (mapInit == null) {
            return;
        }
        int chunkSize = Math.max(1, mapInit.getChunkSize());
        int lastChunkX = Math.max(0, (mapInit.getWidth() - 1) / chunkSize);
        int lastChunkY = Math.max(0, (mapInit.getHeight() - 1) / chunkSize);
        for (int cy = 0; cy <= lastChunkY; cy++) {
            for (int cx = 0; cx <= lastChunkX; cx++) {
                loadedChunks.add(chunkKey(cx, cy));
            }
        }
        pendingChunks.clear();
    }

    private void renderInitialMapLoading(MapInitDto mapInit) {
        updateHudCamera();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(1.25f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Generating and loading world map...", 60f, Gdx.graphics.getHeight() - 80f);
        font.getData().setScale(1.0f);
        font.setColor(Color.LIGHT_GRAY);
        int totalChunks = expectedChunkCount(mapInit);
        int loaded = Math.min(loadedChunks.size(), totalChunks);
        float percent = totalChunks <= 0 ? 0f : (loaded * 100f / totalChunks);
        String size = mapInit == null ? "unknown" : mapInit.getWidth() + "x" + mapInit.getHeight();
        font.draw(batch, "Map: " + size + " | Chunks: " + loaded + "/" + totalChunks + " | Pending: " + pendingChunks.size(), 60f, Gdx.graphics.getHeight() - 120f);
        font.draw(batch, "Progress: " + Math.round(percent) + "%", 60f, Gdx.graphics.getHeight() - 150f);
        font.draw(batch, "The map is preloaded once at startup, so movement will not trigger runtime chunk streaming.", 60f, Gdx.graphics.getHeight() - 185f);
        batch.end();
    }

    private boolean requestChunk(MapInitDto mapInit, int chunkX, int chunkY) {
        int lastChunkX = Math.max(0, (mapInit.getWidth() - 1) / Math.max(1, mapInit.getChunkSize()));
        int lastChunkY = Math.max(0, (mapInit.getHeight() - 1) / Math.max(1, mapInit.getChunkSize()));
        if (chunkX < 0 || chunkY < 0 || chunkX > lastChunkX || chunkY > lastChunkY) {
            return false;
        }

        String key = chunkKey(chunkX, chunkY);
        if (loadedChunks.contains(key) || !pendingChunks.add(key)) {
            return false;
        }

        try {
            MapChunkRequestInputDto input = new MapChunkRequestInputDto(chunkX, chunkX, chunkY, chunkY);
            String payloadJson = mapper.writeValueAsString(input);
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.MAP_CHUNK_REQUEST,
                    cfg.roomId,
                    cfg.clientId,
                    payloadJson
            );
            bus.publishCommand(cfg, commandMessage);
            return true;
        } catch (Exception exception) {
            pendingChunks.remove(key);
            System.err.println("[Client] MAP_CHUNK_REQUEST publish failed:");
            exception.printStackTrace();
            return false;
        }
    }

    private String tileKey(int x, int y) {
        return x + "," + y;
    }

    private String chunkKey(int x, int y) {
        return x + "," + y;
    }

    private float currentTileSize() {
        MapInitDto mapInit = lastMapInit.get();
        return mapInit != null && mapInit.getTileSize() > 0f ? mapInit.getTileSize() : TILE_SIZE;
    }

    private WorldSettings currentSettings() {
        WorldSettings settings = lastWorldSettings.get();
        return settings == null ? WorldSettings.defaults() : settings;
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

    private boolean handleBibbleMenuKey(int keycode) {
        int selectedSlot = hotbarSlotForKey(keycode);
        if (selectedSlot >= 0 && selectedSlot < currentSettings().getMaxActiveBibbles()) {
            sendBibbleAction("TOGGLE_SLOT", "", selectedSlot, "", "", "");
            return true;
        }
        if (keycode == Input.Keys.R) {
            sendBibbleAction("RECALL_ALL", "", -1, "RETURN_TO_TERMINAL", "", "");
            return true;
        }
        if (keycode == Input.Keys.F) {
            sendBibbleAction("FOLLOW_ALL", "", -1, "FOLLOW", "", "");
            return true;
        }
        if (keycode == Input.Keys.S) {
            sendBibbleAction("STAY_ALL", "", -1, "STAY", "", "");
            return true;
        }
        if (keycode == Input.Keys.P) {
            sendBibbleAction("PATROL_ALL", "", -1, "PATROL", "", "");
            return true;
        }
        if (keycode == Input.Keys.W) {
            sendBibbleAction("ASSIGN_NEAREST_WORKSTATION", "", -1, "WORK", "", "");
            return true;
        }
        if (keycode == Input.Keys.C) {
            sendBibbleAction("CAPTURE_NEAREST", "", -1, "", "", "");
            return true;
        }
        if (keycode == Input.Keys.B) {
            bibbleMenuOpen = false;
            return true;
        }
        return true;
    }

    private void sendBibbleAction(String action, String bibbleId, int teamSlot, String command, String targetEntityId, String targetPlayerId) {
        try {
            BibbleInputDto input = new BibbleInputDto(action, bibbleId, teamSlot, command, targetEntityId, targetPlayerId);
            String payloadJson = mapper.writeValueAsString(input);
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.BIBBLE,
                    cfg.roomId,
                    cfg.clientId,
                    payloadJson
            );
            bus.publishCommand(cfg, commandMessage);
        } catch (Exception exception) {
            System.err.println("[Client] BIBBLE publish failed:");
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


    private void craftVisibleRecipe(int visibleRecipeIndex) {
        if (!CraftingMenuRenderer.isCraftingMenuAllowed(currentSettings())) {
            craftingMenuOpen = false;
            return;
        }
        List<CraftingRecipe> recipes = CraftingMenuRenderer.visibleRecipes(currentSettings(), lastSnapshot.get(), cfg.clientId);
        if (visibleRecipeIndex < 0 || visibleRecipeIndex >= recipes.size()) {
            return;
        }

        CraftingRecipe recipe = recipes.get(visibleRecipeIndex);
        sendCraftingAction(recipe);
    }

    private void sendCraftingAction(CraftingRecipe recipe) {
        if (recipe == null) {
            return;
        }
        try {
            CraftingInputDto input = new CraftingInputDto(recipe.getRecipeId(), recipe.getStationType(), "");
            String payloadJson = mapper.writeValueAsString(input);
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.CRAFTING,
                    cfg.roomId,
                    cfg.clientId,
                    payloadJson
            );
            bus.publishCommand(cfg, commandMessage);
        } catch (Exception exception) {
            System.err.println("[Client] CRAFTING publish failed:");
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
        if (craftingMenuOpen || bibbleMenuOpen) {
            return;
        }
        moveRepeatTimer += Gdx.graphics.getDeltaTime();
        WorldSettings settings = currentSettings();
        if (!settings.isMovementEnabled()) {
            return;
        }

        float moveInterval = settings.getMovementRepeatIntervalSeconds() > 0f
                ? settings.getMovementRepeatIntervalSeconds()
                : DEFAULT_MOVE_REPEAT_INTERVAL;
        if (moveRepeatTimer < moveInterval) {
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


    private void handleActionInput() {
        if (craftingMenuOpen || bibbleMenuOpen) {
            return;
        }
        if (!currentSettings().isPlayerInteractionEnabled()) {
            return;
        }
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }

        Vector3 world = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        camera.unproject(world);
        sendAction("ATTACK", world.x, world.y);
    }

    private void sendAction(String action, float targetX, float targetY) {
        try {
            ActionInputDto input = new ActionInputDto(action, targetX, targetY);
            String payloadJson = mapper.writeValueAsString(input);
            CommandMessage commandMessage = new CommandMessage(
                    MessageType.ACTION,
                    cfg.roomId,
                    cfg.clientId,
                    payloadJson
            );
            bus.publishCommand(cfg, commandMessage);
        } catch (Exception exception) {
            System.err.println("[Client] ACTION publish failed:");
            exception.printStackTrace();
        }
    }

    private void handleBuildInput() {
        if (craftingMenuOpen || bibbleMenuOpen) {
            return;
        }
        if (!currentSettings().isBuildingEnabled()) {
            return;
        }
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            return;
        }

        Vector3 world = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        camera.unproject(world);

        float tileSize = currentTileSize();
        int tileX = (int) Math.floor(world.x / tileSize);
        int tileY = (int) Math.floor(world.y / tileSize);

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
