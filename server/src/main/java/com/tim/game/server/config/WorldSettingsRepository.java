package com.tim.game.server.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tim.game.shared.config.WorldSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;

public class WorldSettingsRepository {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public WorldSettings loadOrCreate(String roomId, String explicitPath) {
        return loadOrCreate(roomId, explicitPath, "");
    }

    public WorldSettings loadOrCreate(String roomId, String explicitPath, String worldId) {
        Path path = resolvePath(roomId, explicitPath, worldId);
        try {
            if (Files.exists(path)) {
                WorldSettings loaded = mapper.readValue(path.toFile(), WorldSettings.class);
                WorldSettings normalized = normalizeForPath(loaded == null ? WorldSettings.defaults() : loaded, path);
                save(path, normalized);
                return normalized;
            }

            WorldSettings defaults = WorldSettings.defaults();
            String resolvedWorldId = !isBlank(worldId)
                    ? safeFileName(worldId)
                    : safeFileName(defaultIfBlank(roomId, "1"));
            defaults.setWorldId(resolvedWorldId);
            defaults.setWorldName("Room " + roomId);
            defaults.setMapId("room-" + roomId + "-procedural");
            defaults.setCreatedAt(LocalDateTime.now().toString());
            defaults.setLastPlayedAt(LocalDateTime.now().toString());
            if (isWorldDirectoryPath(path)) {
                Path dir = path.getParent();
                defaults.setWorldSaveDirectory(dir.toString());
                defaults.setMapId("local-" + resolvedWorldId);
                defaults.setMapFile(dir.resolve("map.json").toString());
                defaults.setChunkStreamingEnabled(false);
                defaults.setSaveGeneratedMapOnStart(true);
            }
            WorldSettings normalized = normalizeForPath(defaults, path);
            save(path, normalized);
            return normalized;
        } catch (Exception e) {
            System.err.println("[Server] Could not load world settings from " + path + ": " + e.getMessage());
            return WorldSettings.defaults();
        }
    }

    public Path resolvePath(String roomId, String explicitPath) {
        return resolvePath(roomId, explicitPath, "");
    }

    public Path resolvePath(String roomId, String explicitPath, String worldId) {
        if (!isBlank(explicitPath)) {
            return Path.of(explicitPath.trim());
        }
        if (!isBlank(worldId)) {
            return resolveWorldSettingsPath(worldId);
        }
        Path active = isLocalRoom(roomId) ? resolveActiveWorldSettingsPath() : null;
        if (active != null) {
            return active;
        }
        return Path.of(System.getProperty("user.home"), ".clientgame", "world-settings", safeFileName(roomId) + ".json");
    }

    public Path resolveWorldSettingsPath(String worldId) {
        return Path.of(System.getProperty("user.home"), ".clientgame", "worlds", safeFileName(worldId), "world-settings.json");
    }

    public Path resolveActiveWorldSettingsPath() {
        Path activeFile = Path.of(System.getProperty("user.home"), ".clientgame", "active-local-world.json");
        try {
            if (!Files.exists(activeFile)) {
                return null;
            }
            JsonNode node = mapper.readTree(activeFile.toFile());
            String explicitSettingsPath = node.path("settingsPath").asText("");
            if (!isBlank(explicitSettingsPath)) {
                Path path = Path.of(explicitSettingsPath.trim());
                if (Files.exists(path)) {
                    return path;
                }
            }
            String activeWorldId = node.path("worldId").asText("");
            if (!isBlank(activeWorldId)) {
                Path path = resolveWorldSettingsPath(activeWorldId);
                if (Files.exists(path)) {
                    return path;
                }
            }
        } catch (Exception e) {
            System.err.println("[Server] Could not read active local world file: " + e.getMessage());
        }
        return null;
    }

    public void save(Path path, WorldSettings settings) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            mapper.writeValue(path.toFile(), settings == null ? WorldSettings.defaults() : normalizeForPath(settings, path));
        } catch (Exception e) {
            throw new RuntimeException("Could not save world settings to " + path, e);
        }
    }

    private WorldSettings normalizeForPath(WorldSettings settings, Path path) {
        WorldSettings copy = settings == null ? WorldSettings.defaults() : settings.normalizedCopy();
        if (isWorldDirectoryPath(path)) {
            Path dir = path.getParent();
            String worldId = safeFileName(defaultIfBlank(copy.getWorldId(), dir.getFileName().toString()));
            copy.setWorldId(worldId);
            copy.setWorldSaveDirectory(dir.toString());
            if (isBlank(copy.getMapFile())) {
                copy.setMapFile(dir.resolve("map.json").toString());
            }
            if (isBlank(copy.getMapId()) || "default-procedural".equals(copy.getMapId())) {
                copy.setMapId("local-" + worldId);
            }
            copy.setRandomizeSeed(false);
        }
        return copy.normalizedCopy();
    }

    private boolean isLocalRoom(String roomId) {
        return isBlank(roomId) || "1".equals(roomId.trim()) || "local".equalsIgnoreCase(roomId.trim());
    }

    private boolean isWorldDirectoryPath(Path path) {
        if (path == null || path.getParent() == null || path.getParent().getParent() == null) {
            return false;
        }
        return "worlds".equals(path.getParent().getParent().getFileName().toString());
    }

    private String safeFileName(String value) {
        String source = defaultIfBlank(value, "1").trim().toLowerCase(Locale.ROOT);
        String safe = source.replaceAll("[^a-z0-9._-]", "-").replaceAll("-+", "-");
        return safe.isBlank() || "-".equals(safe) ? "1" : safe;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
