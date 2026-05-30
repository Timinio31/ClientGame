package com.tim.game.client.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientSettingsRepository {
    private static final String APP_FOLDER_NAME = ".clientgame";
    private static final String SETTINGS_FILE_NAME = "client-settings.json";

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public ClientSettings load() {
        Path file = getSettingsFile();
        if (!Files.exists(file)) {
            ClientSettings defaults = new ClientSettings();
            save(defaults);
            return defaults;
        }

        try {
            return mapper.readValue(file.toFile(), ClientSettings.class).normalizedCopy();
        } catch (IOException exception) {
            System.err.println("[ClientSettingsRepository] Failed to load settings, using defaults: " + exception.getMessage());
            return new ClientSettings();
        }
    }

    public void save(ClientSettings settings) {
        try {
            Files.createDirectories(getAppFolder());
            mapper.writeValue(getSettingsFile().toFile(), settings == null ? new ClientSettings() : settings.normalizedCopy());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save client settings", exception);
        }
    }

    public Path getAppFolder() {
        return Path.of(System.getProperty("user.home"), APP_FOLDER_NAME);
    }

    public Path getSettingsFile() {
        return getAppFolder().resolve(SETTINGS_FILE_NAME);
    }

    public Path getTexturePackFolder() {
        return getAppFolder().resolve("texture-packs");
    }
}
