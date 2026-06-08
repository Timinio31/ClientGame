package com.tim.game.client.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.shared.config.WorldSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LocalWorldRepository {
    public static final String LOCAL_ROOM_ID = "1";

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path baseDirectory;
    private final Path activeWorldFile;
    private final WorldSettingsFileRepository roomSettingsRepository = new WorldSettingsFileRepository();

    public LocalWorldRepository() {
        this(Path.of(System.getProperty("user.home"), ".clientgame", "worlds"),
                Path.of(System.getProperty("user.home"), ".clientgame", "active-local-world.json"));
    }

    public LocalWorldRepository(Path baseDirectory, Path activeWorldFile) {
        this.baseDirectory = baseDirectory;
        this.activeWorldFile = activeWorldFile;
    }

    public List<LocalWorldProfile> loadWorlds() {
        List<LocalWorldProfile> worlds = new ArrayList<>();
        try {
            if (!Files.exists(baseDirectory)) {
                return worlds;
            }
            try (var stream = Files.list(baseDirectory)) {
                stream.filter(Files::isDirectory)
                        .sorted(Comparator.comparing(Path::getFileName))
                        .forEach(path -> {
                            LocalWorldProfile profile = loadWorld(path.getFileName().toString());
                            if (profile != null) {
                                worlds.add(profile);
                            }
                        });
            }
            worlds.sort((a, b) -> b.getWorldSettings().getLastPlayedAt().compareTo(a.getWorldSettings().getLastPlayedAt()));
            return worlds;
        } catch (Exception e) {
            System.err.println("[Client] Could not list local worlds: " + e.getMessage());
            return worlds;
        }
    }

    public LocalWorldProfile loadWorld(String worldId) {
        String safeId = safeWorldId(worldId);
        Path dir = resolveWorldDirectory(safeId);
        Path settingsPath = dir.resolve("world-settings.json");
        Path mapPath = dir.resolve("map.json");
        try {
            if (!Files.exists(settingsPath)) {
                return null;
            }
            WorldSettings settings = mapper.readValue(settingsPath.toFile(), WorldSettings.class).normalizedCopy();
            settings = normalizeForWorldDirectory(safeId, settings, dir);
            return new LocalWorldProfile(safeId, settings.getWorldName(), dir, settingsPath, mapPath, settings);
        } catch (Exception e) {
            System.err.println("[Client] Could not load local world " + safeId + ": " + e.getMessage());
            return null;
        }
    }

    public LocalWorldProfile createWorld(String requestedName, int width, int height, long seed) {
        String name = requestedName == null || requestedName.isBlank()
                ? "New World"
                : requestedName.trim();
        String baseId = safeWorldId(name);
        String worldId = uniqueWorldId(baseId);
        Path dir = resolveWorldDirectory(worldId);
        long resolvedSeed = seed == 0L ? System.currentTimeMillis() : seed;
        String now = LocalDateTime.now().toString();

        WorldSettings settings = WorldSettings.defaults();
        settings.setWorldId(worldId);
        settings.setWorldName(name);
        settings.setDescription("Local world save stored in " + dir);
        settings.setGameMode("SANDBOX");
        settings.setMapMode(WorldSettings.MAP_MODE_PROCEDURAL);
        settings.setMapId("local-" + worldId);
        settings.setMapWidth(width);
        settings.setMapHeight(height);
        settings.setSeed(resolvedSeed);
        settings.setRandomizeSeed(false);
        settings.setWorldSaveDirectory(dir.toString());
        settings.setMapFile(dir.resolve("map.json").toString());
        settings.setCreatedAt(now);
        settings.setLastPlayedAt(now);
        settings.setChunkStreamingEnabled(false);
        settings.setSaveGeneratedMapOnStart(true);
        settings = settings.normalizedCopy();

        saveWorldSettings(worldId, settings);
        return loadWorld(worldId);
    }

    public void saveWorldSettings(String worldId, WorldSettings settings) {
        String safeId = safeWorldId(worldId);
        Path dir = resolveWorldDirectory(safeId);
        try {
            Files.createDirectories(dir);
            WorldSettings normalized = normalizeForWorldDirectory(safeId, settings, dir).normalizedCopy();
            mapper.writeValue(dir.resolve("world-settings.json").toFile(), normalized);
        } catch (Exception e) {
            throw new RuntimeException("Could not save local world settings for " + safeId, e);
        }
    }

    public LocalWorldProfile selectForLocalPlay(LocalWorldProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("No local world selected");
        }
        String safeId = safeWorldId(profile.getWorldId());
        Path dir = resolveWorldDirectory(safeId);
        WorldSettings settings = normalizeForWorldDirectory(safeId, profile.getWorldSettings(), dir).normalizedCopy();
        settings.setLastPlayedAt(LocalDateTime.now().toString());
        settings = settings.normalizedCopy();
        saveWorldSettings(safeId, settings);
        writeActiveWorld(safeId, dir.resolve("world-settings.json"));
        roomSettingsRepository.saveForRoom(LOCAL_ROOM_ID, settings);
        return loadWorld(safeId);
    }

    public ClientConfig buildLocalClientConfig(String clientId) {
        return ClientConfig.localDefault(clientId);
    }

    public Path getBaseDirectory() {
        return baseDirectory;
    }

    public Path getActiveWorldFile() {
        return activeWorldFile;
    }

    public Path resolveWorldDirectory(String worldId) {
        return baseDirectory.resolve(safeWorldId(worldId));
    }

    private WorldSettings normalizeForWorldDirectory(String worldId, WorldSettings settings, Path dir) {
        WorldSettings source = settings == null ? WorldSettings.defaults() : settings.normalizedCopy();
        String safeId = safeWorldId(worldId == null || worldId.isBlank() ? source.getWorldId() : worldId);
        source.setWorldId(safeId);
        source.setWorldSaveDirectory(dir.toString());
        if (source.getMapId() == null || source.getMapId().isBlank() || "default-procedural".equals(source.getMapId())) {
            source.setMapId("local-" + safeId);
        }
        if (source.getMapFile() == null || source.getMapFile().isBlank()) {
            source.setMapFile(dir.resolve("map.json").toString());
        }
        source.setRandomizeSeed(false);
        return source.normalizedCopy();
    }

    private void writeActiveWorld(String worldId, Path settingsPath) {
        try {
            if (activeWorldFile.getParent() != null) {
                Files.createDirectories(activeWorldFile.getParent());
            }
            ActiveWorldData data = new ActiveWorldData();
            data.worldId = safeWorldId(worldId);
            data.settingsPath = settingsPath.toString();
            data.selectedAt = LocalDateTime.now().toString();
            mapper.writeValue(activeWorldFile.toFile(), data);
        } catch (Exception e) {
            throw new RuntimeException("Could not write active local world file " + activeWorldFile, e);
        }
    }

    private String uniqueWorldId(String baseId) {
        String cleanBase = safeWorldId(baseId);
        String candidate = cleanBase;
        int counter = 2;
        while (Files.exists(resolveWorldDirectory(candidate))) {
            candidate = cleanBase + "-" + counter;
            counter++;
        }
        return candidate;
    }

    private String safeWorldId(String value) {
        String source = value == null || value.isBlank() ? "local-world" : value.trim().toLowerCase(Locale.ROOT);
        String safe = source.replaceAll("[^a-z0-9._-]", "-").replaceAll("-+", "-");
        if (safe.isBlank() || "-".equals(safe)) {
            return "local-world";
        }
        return safe;
    }

    public String describeActiveWorld() {
        try {
            if (!Files.exists(activeWorldFile)) {
                return "none";
            }
            JsonNode node = mapper.readTree(activeWorldFile.toFile());
            String worldId = node.path("worldId").asText("none");
            String selectedAt = node.path("selectedAt").asText("");
            return selectedAt.isBlank() ? worldId : worldId + " @ " + selectedAt;
        } catch (Exception e) {
            return "unreadable: " + e.getMessage();
        }
    }

    private static class ActiveWorldData {
        public String worldId = "";
        public String settingsPath = "";
        public String selectedAt = "";
    }
}
