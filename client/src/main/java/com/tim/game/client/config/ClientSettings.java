package com.tim.game.client.config;

public class ClientSettings {
    public static final String DEFAULT_TEXTURE_PACK_ID = "midcentury_platinum";

    private String texturePackId = DEFAULT_TEXTURE_PACK_ID;

    public ClientSettings() {
    }

    public ClientSettings(String texturePackId) {
        this.texturePackId = normalizeTexturePackId(texturePackId);
    }

    public String getTexturePackId() {
        return normalizeTexturePackId(texturePackId);
    }

    public void setTexturePackId(String texturePackId) {
        this.texturePackId = normalizeTexturePackId(texturePackId);
    }

    public ClientSettings normalizedCopy() {
        return new ClientSettings(getTexturePackId());
    }

    private static String normalizeTexturePackId(String value) {
        return value == null || value.isBlank() ? DEFAULT_TEXTURE_PACK_ID : value.trim();
    }
}
