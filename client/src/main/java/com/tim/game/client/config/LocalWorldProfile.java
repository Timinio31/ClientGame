package com.tim.game.client.config;

import com.tim.game.shared.config.WorldSettings;

import java.nio.file.Path;

public class LocalWorldProfile {
    private final String worldId;
    private final String displayName;
    private final Path worldDirectory;
    private final Path settingsFile;
    private final Path mapFile;
    private final WorldSettings worldSettings;

    public LocalWorldProfile(String worldId,
                             String displayName,
                             Path worldDirectory,
                             Path settingsFile,
                             Path mapFile,
                             WorldSettings worldSettings) {
        this.worldId = worldId == null ? "default-world" : worldId;
        this.displayName = displayName == null || displayName.isBlank() ? this.worldId : displayName.trim();
        this.worldDirectory = worldDirectory;
        this.settingsFile = settingsFile;
        this.mapFile = mapFile;
        this.worldSettings = worldSettings == null ? WorldSettings.defaults() : worldSettings.normalizedCopy();
    }

    public String getWorldId() {
        return worldId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Path getWorldDirectory() {
        return worldDirectory;
    }

    public Path getSettingsFile() {
        return settingsFile;
    }

    public Path getMapFile() {
        return mapFile;
    }

    public WorldSettings getWorldSettings() {
        return worldSettings;
    }
}
