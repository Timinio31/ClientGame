package com.tim.game.client.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.client.texture.TextureKeys;
import com.tim.game.client.texture.TexturePackManager;
import com.tim.game.shared.DTOs.update.BuildingStateDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.TileStateDto;
import com.tim.game.shared.DTOs.update.WorldItemStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;
import com.tim.game.shared.model.Vector2f;

import java.util.HashMap;
import java.util.Map;

public final class WorldRenderer {

    private static final float FALLBACK_TILE_SIZE = 1.0f;
    private static final float PLAYER_WIDTH = 1.15f;
    private static final float PLAYER_HEIGHT = 1.20f;
    private static final float MOVE_VISUAL_DURATION = 0.18f;

    private static final Map<String, PlayerVisualState> PLAYER_VISUALS = new HashMap<>();
    private static float animationClock;

    private WorldRenderer() {
    }

    public static void renderWorld(OrthographicCamera camera,
                                   ShapeRenderer shapes,
                                   SpriteBatch batch,
                                   MapInitDto mapInit,
                                   WorldSnapshotDto snapshot,
                                   String localClientId) {
        float tileSize = mapInit != null && mapInit.getTileSize() > 0f ? mapInit.getTileSize() : FALLBACK_TILE_SIZE;
        animationClock += Gdx.graphics.getDeltaTime();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        renderTiles(batch, mapInit, tileSize);
        renderWorldItems(batch, snapshot, tileSize);
        renderBuildings(batch, snapshot, tileSize);
        renderPlayers(batch, snapshot, localClientId);
        batch.end();

        if (DebugConfig.isEnabled(DebugCategory.RENDER)) {
            renderGrid(camera, shapes, tileSize);
        }
    }

    private static void renderTiles(SpriteBatch batch, MapInitDto mapInit, float tileSize) {
        if (mapInit == null || mapInit.getTiles() == null) {
            return;
        }

        TexturePackManager texturePackManager = TexturePackManager.getInstance();
        for (TileStateDto tile : mapInit.getTiles()) {
            if (tile == null) {
                continue;
            }

            Texture texture = texturePackManager.getTileTexture(tile.getType(), animationClock);
            batch.draw(texture, tile.getX() * tileSize, tile.getY() * tileSize, tileSize, tileSize);
        }
    }


    private static void renderWorldItems(SpriteBatch batch,
                                         WorldSnapshotDto snapshot,
                                         float tileSize) {
        if (snapshot == null || snapshot.getWorldItems() == null) {
            return;
        }

        TexturePackManager texturePackManager = TexturePackManager.getInstance();
        for (WorldItemStateDto worldItem : snapshot.getWorldItems()) {
            if (worldItem == null || worldItem.getPosition() == null || worldItem.getItem() == null) {
                continue;
            }

            Texture texture = texturePackManager.getItemTexture(worldItem.getItem().getItemType());
            float size = tileSize * 0.58f;
            float x = worldItem.getPosition().getX() - size * 0.5f;
            float y = worldItem.getPosition().getY() - size * 0.5f;
            batch.draw(texture, x, y, size, size);
        }
    }

    private static void renderPlayers(SpriteBatch batch,
                                      WorldSnapshotDto snapshot,
                                      String localClientId) {
        if (snapshot == null || snapshot.getPlayers() == null) {
            return;
        }

        TexturePackManager texturePackManager = TexturePackManager.getInstance();
        for (PlayerStateDto player : snapshot.getPlayers()) {
            if (player == null || player.getPosition() == null) {
                continue;
            }

            String playerKey = player.getEntityId() != null ? player.getEntityId() : player.getClientId();
            PlayerVisualState visualState = PLAYER_VISUALS.computeIfAbsent(playerKey, ignored -> new PlayerVisualState());
            updatePlayerVisualState(visualState, player.getPosition());

            boolean isLocalPlayer = localClientId != null && localClientId.equals(player.getClientId());
            Texture texture = texturePackManager.getPlayerTexture(isLocalPlayer, visualState.direction, visualState.moving, animationClock);

            float width = PLAYER_WIDTH;
            float height = PLAYER_HEIGHT;
            float x = player.getPosition().getX() - width * 0.5f;
            float y = player.getPosition().getY() - height * 0.72f;
            batch.draw(texture, x, y, width, height);
        }
    }

    private static void updatePlayerVisualState(PlayerVisualState visualState, Vector2f position) {
        if (!visualState.initialized) {
            visualState.initialized = true;
            visualState.lastX = position.getX();
            visualState.lastY = position.getY();
            visualState.direction = TextureKeys.DIR_DOWN;
            visualState.moving = false;
            visualState.lastMoveTime = animationClock;
            return;
        }

        float dx = position.getX() - visualState.lastX;
        float dy = position.getY() - visualState.lastY;
        if (Math.abs(dx) > 0.001f || Math.abs(dy) > 0.001f) {
            visualState.direction = determineDirection(dx, dy);
            visualState.lastX = position.getX();
            visualState.lastY = position.getY();
            visualState.lastMoveTime = animationClock;
            visualState.moving = true;
        } else {
            visualState.moving = animationClock - visualState.lastMoveTime <= MOVE_VISUAL_DURATION;
        }
    }

    private static String determineDirection(float dx, float dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx < 0f ? TextureKeys.DIR_LEFT : TextureKeys.DIR_RIGHT;
        }
        return dy < 0f ? TextureKeys.DIR_DOWN : TextureKeys.DIR_UP;
    }

    private static void renderBuildings(SpriteBatch batch,
                                        WorldSnapshotDto snapshot,
                                        float tileSize) {
        if (snapshot == null || snapshot.getBuildings() == null) {
            return;
        }

        TexturePackManager texturePackManager = TexturePackManager.getInstance();
        for (BuildingStateDto building : snapshot.getBuildings()) {
            if (building == null || building.getPosition() == null) {
                continue;
            }

            Texture texture = texturePackManager.getBuildingTexture(building.getBuildingType(), animationClock);
            BuildingDimensions dimensions = getBuildingDimensions(building.getBuildingType(), tileSize);
            float centerX = building.getPosition().getX();
            float centerY = building.getPosition().getY();
            float x = centerX - dimensions.width * 0.5f;
            float y = centerY + tileSize * 0.18f - dimensions.height;
            batch.draw(texture, x, y, dimensions.width, dimensions.height);
        }
    }

    private static BuildingDimensions getBuildingDimensions(String buildingType, float tileSize) {
        if (buildingType == null) {
            return new BuildingDimensions(tileSize * 1.35f, tileSize * 1.65f);
        }

        return switch (buildingType.toUpperCase()) {
            case "GENERATOR" -> new BuildingDimensions(tileSize * 1.60f, tileSize * 1.85f);
            case "CITY_HOUSE_SMALL" -> new BuildingDimensions(tileSize * 1.60f, tileSize * 1.60f);
            case "CITY_HOUSE_TALL" -> new BuildingDimensions(tileSize * 1.55f, tileSize * 2.05f);
            case "CITY_SHOP" -> new BuildingDimensions(tileSize * 1.90f, tileSize * 1.55f);
            case "CITY_HALL" -> new BuildingDimensions(tileSize * 2.25f, tileSize * 2.05f);
            case "CITY_WAREHOUSE" -> new BuildingDimensions(tileSize * 2.10f, tileSize * 1.55f);
            default -> new BuildingDimensions(tileSize * 1.35f, tileSize * 1.65f);
        };
    }

    private static void renderGrid(OrthographicCamera camera, ShapeRenderer shapes, float tileSize) {
        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        float left = camera.position.x - halfW;
        float right = camera.position.x + halfW;
        float bottom = camera.position.y - halfH;
        float top = camera.position.y + halfH;

        int startX = (int) Math.floor(left / tileSize) - 1;
        int endX = (int) Math.floor(right / tileSize) + 1;
        int startY = (int) Math.floor(bottom / tileSize) - 1;
        int endY = (int) Math.floor(top / tileSize) + 1;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(new Color(0f, 0f, 0f, 0.20f));

        for (int x = startX; x <= endX; x++) {
            float wx = x * tileSize;
            shapes.line(wx, startY * tileSize, wx, endY * tileSize);
        }

        for (int y = startY; y <= endY; y++) {
            float wy = y * tileSize;
            shapes.line(startX * tileSize, wy, endX * tileSize, wy);
        }

        shapes.end();
    }

    public static Vector2f getLocalPlayerPos(WorldSnapshotDto snapshot, String localClientId) {
        if (snapshot == null || snapshot.getPlayers() == null || localClientId == null) {
            return null;
        }

        for (PlayerStateDto player : snapshot.getPlayers()) {
            if (player == null || player.getPosition() == null) {
                continue;
            }

            if (localClientId.equals(player.getClientId())) {
                return player.getPosition();
            }
        }

        return null;
    }

    private static final class PlayerVisualState {
        private boolean initialized;
        private float lastX;
        private float lastY;
        private float lastMoveTime;
        private boolean moving;
        private String direction = TextureKeys.DIR_DOWN;
    }

    private static final class BuildingDimensions {
        private final float width;
        private final float height;

        private BuildingDimensions(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }
}
