package com.tim.game.client.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SavedServerRepository {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path saveFile;
    private final WorldSettingsFileRepository worldSettingsFileRepository = new WorldSettingsFileRepository();

    public SavedServerRepository() {
        this(Path.of(System.getProperty("user.home"), ".clientgame", "saved-servers.json"));
    }

    public SavedServerRepository(Path saveFile) {
        this.saveFile = saveFile;
    }

    public List<ServerProfile> loadProfiles() {
        try {
            if (!Files.exists(saveFile)) {
                return new ArrayList<>();
            }
            List<ServerProfile> profiles = mapper.readValue(saveFile.toFile(), new TypeReference<List<ServerProfile>>() {});
            return profiles == null ? new ArrayList<>() : profiles;
        } catch (Exception e) {
            System.err.println("[Client] Could not load saved servers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void saveProfiles(List<ServerProfile> profiles) {
        try {
            Files.createDirectories(saveFile.getParent());
            mapper.writeValue(saveFile.toFile(), profiles == null ? new ArrayList<>() : profiles);
        } catch (Exception e) {
            throw new RuntimeException("Could not save server profiles to " + saveFile, e);
        }
    }

    public void upsert(ServerProfile profile) {
        ServerProfile normalized = profile.normalizedCopy();
        normalized.setLastUsedAt(LocalDateTime.now().toString());

        List<ServerProfile> profiles = loadProfiles();
        profiles.removeIf(existing -> existing.normalizedCopy().getConnectionKey().equals(normalized.getConnectionKey()));
        profiles.add(0, normalized);
        saveProfiles(profiles);
        worldSettingsFileRepository.saveForRoom(normalized.getRoomId(), normalized.getWorldSettings());
    }

    public void clear() {
        saveProfiles(new ArrayList<>());
    }

    public Path getSaveFile() {
        return saveFile;
    }
}
