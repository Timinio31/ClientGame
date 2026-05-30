package com.tim.game.client.net;

import com.tim.game.client.config.ServerProfile;

public class ClientConfig {
    public final String rabbitHost;
    public final int rabbitPort;
    public final String username;
    public final String password;
    public final String virtualHost;

    public final String roomId;
    public final String clientId;
    public final String roomPassword;

    public ClientConfig(String rabbitHost,
                        int rabbitPort,
                        String username,
                        String password,
                        String virtualHost,
                        String roomId,
                        String clientId,
                        String roomPassword) {
        this.rabbitHost = defaultIfBlank(rabbitHost, "localhost");
        this.rabbitPort = rabbitPort > 0 ? rabbitPort : 5672;
        this.username = defaultIfBlank(username, "guest");
        this.password = defaultIfBlank(password, "guest");
        this.virtualHost = defaultIfBlank(virtualHost, "/");
        this.roomId = defaultIfBlank(roomId, "1");
        this.clientId = defaultIfBlank(clientId, "c" + System.currentTimeMillis());
        this.roomPassword = roomPassword == null ? "" : roomPassword;
    }

    public static ClientConfig localDefault(String clientId) {
        return new ClientConfig(
                "localhost",
                5672,
                "guest",
                "guest",
                "/",
                "1",
                clientId,
                ""
        );
    }

    public static ClientConfig fromServerProfile(ServerProfile profile, String clientId) {
        ServerProfile normalized = profile.normalizedCopy();
        return new ClientConfig(
                normalized.getHost(),
                normalized.getPort(),
                normalized.getUsername(),
                normalized.getPassword(),
                normalized.getVirtualHost(),
                normalized.getRoomId(),
                clientId,
                normalized.getRoomPassword()
        );
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
