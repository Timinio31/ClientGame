package com.tim.game.client.config;

import com.tim.game.shared.config.WorldSettings;

public class ServerProfile {
    private String name;
    private String host;
    private int port;
    private String username;
    private String password;
    private String virtualHost;
    private String roomId;
    private String roomPassword;
    private String lastUsedAt;
    private WorldSettings worldSettings = WorldSettings.defaults();

    public ServerProfile() {
    }

    public ServerProfile(String name,
                         String host,
                         int port,
                         String username,
                         String password,
                         String virtualHost,
                         String roomId,
                         String roomPassword) {
        this(name, host, port, username, password, virtualHost, roomId, roomPassword, WorldSettings.defaults());
    }

    public ServerProfile(String name,
                         String host,
                         int port,
                         String username,
                         String password,
                         String virtualHost,
                         String roomId,
                         String roomPassword,
                         WorldSettings worldSettings) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;
        this.roomId = roomId;
        this.roomPassword = roomPassword;
        setWorldSettings(worldSettings);
    }

    public static ServerProfile localDefault() {
        return new ServerProfile(
                "Local Test Server",
                "localhost",
                5672,
                "guest",
                "guest",
                "/",
                "1",
                "",
                WorldSettings.defaults()
        );
    }

    public ServerProfile normalizedCopy() {
        ServerProfile copy = new ServerProfile();
        copy.setName(defaultIfBlank(name, "Unnamed Server"));
        copy.setHost(defaultIfBlank(host, "localhost"));
        copy.setPort(port > 0 ? port : 5672);
        copy.setUsername(defaultIfBlank(username, "guest"));
        copy.setPassword(defaultIfBlank(password, "guest"));
        copy.setVirtualHost(defaultIfBlank(virtualHost, "/"));
        copy.setRoomId(defaultIfBlank(roomId, "1"));
        copy.setRoomPassword(roomPassword == null ? "" : roomPassword);
        copy.setLastUsedAt(lastUsedAt);
        copy.setWorldSettings(worldSettings == null ? WorldSettings.defaults() : worldSettings.normalizedCopy());
        return copy;
    }

    public String getConnectionKey() {
        ServerProfile normalized = normalizedCopy();
        return normalized.host + ":" + normalized.port + ":" + normalized.virtualHost + ":" + normalized.roomId;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomPassword() {
        return roomPassword;
    }

    public void setRoomPassword(String roomPassword) {
        this.roomPassword = roomPassword;
    }

    public String getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(String lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public WorldSettings getWorldSettings() {
        return worldSettings;
    }

    public void setWorldSettings(WorldSettings worldSettings) {
        this.worldSettings = worldSettings == null ? WorldSettings.defaults() : worldSettings.normalizedCopy();
    }
}
