package com.tim.game.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.shared.DTOs.update.BuildingStateDto;
import com.tim.game.shared.DTOs.update.PlayerStateDto;
import com.tim.game.shared.DTOs.update.TileStateDto;
import com.tim.game.shared.DTOs.update.WorldSnapshotDto;
import com.tim.game.shared.model.Vector2f;

public final class WorldRenderer {

    private static final float TILE_SIZE = 1.0f;
    private static final float PLAYER_RADIUS = 0.30f;

    private WorldRenderer() {
    }

    public static void renderWorld(OrthographicCamera camera, ShapeRenderer shapes, WorldSnapshotDto snapshot, String localClientId) {
        // Hintergrund
        shapes.setProjectionMatrix(camera.combined);

        renderTiles(camera, shapes, snapshot);
        renderGrid(camera, shapes, TILE_SIZE);
        renderBuildings(camera, shapes, snapshot);
        renderPlayers(camera, shapes, snapshot, localClientId);
    }

    private static void renderTiles(OrthographicCamera camera, ShapeRenderer shapes, WorldSnapshotDto snapshot) {
        if (snapshot == null || snapshot.getMap() == null || snapshot.getMap().getTiles() == null) {
            return;
        }

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (TileStateDto tile : snapshot.getMap().getTiles()) {
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

            shapes.rect(tile.getX(), tile.getY(), TILE_SIZE, TILE_SIZE);
        }

        shapes.end();
    }

    private static void renderPlayers(OrthographicCamera camera, ShapeRenderer shapes, WorldSnapshotDto snapshot, String localClientId) {
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

            if (isLocalPlayer) {
                shapes.setColor(Color.LIME);
            } else {
                shapes.setColor(Color.RED);
            }

            float x = player.getPosition().getX();
            float y = player.getPosition().getY();
            shapes.circle(x, y, PLAYER_RADIUS, 24);
        }

        shapes.end();
    }

    private static void renderBuildings(OrthographicCamera camera, ShapeRenderer shapes, WorldSnapshotDto snapshot) {
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

            shapes.rect(centerX - TILE_SIZE * 0.5f, centerY - TILE_SIZE * 0.5f, TILE_SIZE, TILE_SIZE);
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
        if (snapshot == null || snapshot.getPlayers() == null) {
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