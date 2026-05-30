package com.tim.game.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.shared.DTOs.update.BuildingStateDto;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.TileStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.debug.DebugCategory;
import com.tim.game.shared.debug.DebugConfig;
import com.tim.game.shared.model.Vector2f;

public final class WorldRenderer {

    private static final float FALLBACK_TILE_SIZE = 1.0f;
    private static final float PLAYER_RADIUS = 0.30f;

    private WorldRenderer() {
    }

    public static void renderWorld(OrthographicCamera camera,
                                   ShapeRenderer shapes,
                                   MapInitDto mapInit,
                                   WorldSnapshotDto snapshot,
                                   String localClientId) {
        shapes.setProjectionMatrix(camera.combined);

        renderTiles(camera, shapes, mapInit);

        float tileSize = mapInit != null ? mapInit.getTileSize() : FALLBACK_TILE_SIZE;
        if (DebugConfig.isEnabled(DebugCategory.RENDER)) {
            renderGrid(camera, shapes, tileSize);
        }

        renderBuildings(camera, shapes, snapshot, tileSize);
        renderPlayers(camera, shapes, snapshot, localClientId);
    }

    private static void renderTiles(OrthographicCamera camera, ShapeRenderer shapes, MapInitDto mapInit) {
        if (mapInit == null || mapInit.getTiles() == null) {
            return;
        }

        float tileSize = mapInit.getTileSize() > 0f ? mapInit.getTileSize() : FALLBACK_TILE_SIZE;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (TileStateDto tile : mapInit.getTiles()) {
            if (tile == null) {
                continue;
            }

            String type = tile.getType();
            if (type == null) {
                shapes.setColor(Color.MAGENTA);
            } else {
                switch (type) {
                    case "WALL" -> shapes.setColor(0.30f, 0.30f, 0.30f, 1f);
                    case "GRASS" -> shapes.setColor(0.20f, 0.60f, 0.20f, 1f);
                    case "WATER" -> shapes.setColor(0.20f, 0.40f, 0.85f, 1f);
                    case "SPAWN" -> shapes.setColor(1.00f, 0.90f, 0.20f, 1f);
                    default -> shapes.setColor(Color.MAGENTA);
                }
            }

            shapes.rect(tile.getX() * tileSize, tile.getY() * tileSize, tileSize, tileSize);
        }

        shapes.end();
    }

    private static void renderPlayers(OrthographicCamera camera,
                                      ShapeRenderer shapes,
                                      WorldSnapshotDto snapshot,
                                      String localClientId) {
        if (snapshot == null || snapshot.getPlayers() == null) {
            return;
        }

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (PlayerStateDto player : snapshot.getPlayers()) {
            if (player == null || player.getPosition() == null) {
                continue;
            }

            boolean isLocalPlayer = localClientId != null && localClientId.equals(player.getClientId());
            shapes.setColor(isLocalPlayer ? Color.LIME : Color.RED);

            float x = player.getPosition().getX();
            float y = player.getPosition().getY();
            shapes.circle(x, y, PLAYER_RADIUS, 24);
        }

        shapes.end();
    }

    private static void renderBuildings(OrthographicCamera camera,
                                        ShapeRenderer shapes,
                                        WorldSnapshotDto snapshot,
                                        float tileSize) {
        if (snapshot == null || snapshot.getBuildings() == null) {
            return;
        }

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.ORANGE);

        for (BuildingStateDto building : snapshot.getBuildings()) {
            if (building == null || building.getPosition() == null) {
                continue;
            }

            float centerX = building.getPosition().getX();
            float centerY = building.getPosition().getY();
            shapes.rect(centerX - tileSize * 0.5f, centerY - tileSize * 0.5f, tileSize, tileSize);
        }

        shapes.end();
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
        shapes.setColor(0f, 0f, 0f, 0.20f);

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
}
