package com.tim.game.client.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tim.game.shared.config.WorldSettings;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the same JSON format that the server reads through WorldSettingsRepository.
 * For local development this means: create/save a profile in the client, then start :server:run with the same room id.
 */
public class WorldSettingsFileRepository {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void saveForRoom(String roomId, WorldSettings settings) {
        save(resolveRoomSettingsPath(roomId), settings);
    }

    public void save(Path path, WorldSettings settings) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            mapper.writeValue(path.toFile(), settings == null ? WorldSettings.defaults() : settings.normalizedCopy());
        } catch (Exception e) {
            throw new RuntimeException("Could not save world settings to " + path, e);
        }
    }

    public WorldSettings loadForRoom(String roomId) {
        Path path = resolveRoomSettingsPath(roomId);
        try {
            if (!Files.exists(path)) {
                return WorldSettings.defaults();
            }
            WorldSettings loaded = mapper.readValue(path.toFile(), WorldSettings.class);
            return loaded == null ? WorldSettings.defaults() : loaded.normalizedCopy();
        } catch (Exception e) {
            System.err.println("[Client] Could not load world settings from " + path + ": " + e.getMessage());
            return WorldSettings.defaults();
        }
    }

    public Path resolveRoomSettingsPath(String roomId) {
        return Path.of(System.getProperty("user.home"), ".clientgame", "world-settings", safeFileName(roomId) + ".json");
    }

    private String safeFileName(String value) {
        String source = value == null || value.isBlank() ? "1" : value.trim();
        return source.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
