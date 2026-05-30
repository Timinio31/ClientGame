package com.tim.game.client.texture;

import java.nio.file.Path;

public class TexturePackDefinition {
    private final String id;
    private final String displayName;
    private final String description;
    private final boolean builtIn;
    private final boolean generated;
    private final Path rootPath;

    public TexturePackDefinition(String id,
                                 String displayName,
                                 String description,
                                 boolean builtIn,
                                 boolean generated,
                                 Path rootPath) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.builtIn = builtIn;
        this.generated = generated;
        this.rootPath = rootPath;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public boolean isGenerated() {
        return generated;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public String getSourceLabel() {
        if (generated) {
            return "Generated fallback";
        }
        if (builtIn) {
            return "Built-in";
        }
        return rootPath == null ? "External" : "External: " + rootPath;
    }
}
