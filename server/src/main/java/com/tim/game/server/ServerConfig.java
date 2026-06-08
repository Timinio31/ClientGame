package com.tim.game.server;

public class ServerConfig {
    /**
     * rabbitHost → wohin soll sich der Server verbinden? (bei dir: "localhost")
     * rabbitPort → Standard-RabbitMQ-Port: 5672
     * rabbitUsername / rabbitPassword → aktuell guest/guest (oder dein eigener User)
     * rabbitVirtualHost → / oder z. B. clientgame, falls du einen eigenen VHost angelegt hast
     * roomId → brauchen wir für Queue-/Routing-Key-Namen
     * worldSettingsPath → optionaler Pfad zu einer JSON-Datei mit den erweiterten Welt-/Serverregeln
     * worldId → optionaler lokaler Weltordner unter ~/.clientgame/worlds/<worldId>
     */
    private final String rabbitHost;
    private final int rabbitPort;
    private final String rabbitUsername;
    private final String rabbitPassword;
    private final String rabbitVirtualHost;
    private final String roomId;
    private final String worldSettingsPath;
    private final String worldId;

    public ServerConfig(String rabbitHost,
                        int rabbitPort,
                        String rabbitUsername,
                        String rabbitPassword,
                        String rabbitVirtualHost,
                        String roomId) {
        this(rabbitHost, rabbitPort, rabbitUsername, rabbitPassword, rabbitVirtualHost, roomId, "", "");
    }

    public ServerConfig(String rabbitHost,
                        int rabbitPort,
                        String rabbitUsername,
                        String rabbitPassword,
                        String rabbitVirtualHost,
                        String roomId,
                        String worldSettingsPath) {
        this(rabbitHost, rabbitPort, rabbitUsername, rabbitPassword, rabbitVirtualHost, roomId, worldSettingsPath, "");
    }

    public ServerConfig(String rabbitHost,
                        int rabbitPort,
                        String rabbitUsername,
                        String rabbitPassword,
                        String rabbitVirtualHost,
                        String roomId,
                        String worldSettingsPath,
                        String worldId) {
        this.rabbitHost = defaultIfBlank(rabbitHost, "localhost");
        this.rabbitPort = rabbitPort > 0 ? rabbitPort : 5672;
        this.rabbitUsername = defaultIfBlank(rabbitUsername, "guest");
        this.rabbitPassword = defaultIfBlank(rabbitPassword, "guest");
        this.rabbitVirtualHost = defaultIfBlank(rabbitVirtualHost, "/");
        this.roomId = defaultIfBlank(roomId, "1");
        this.worldSettingsPath = worldSettingsPath == null ? "" : worldSettingsPath.trim();
        this.worldId = worldId == null ? "" : worldId.trim();
    }

    public String getRabbitHost() {
        return rabbitHost;
    }

    public int getRabbitPort() {
        return rabbitPort;
    }

    public String getRabbitUsername() {
        return rabbitUsername;
    }

    public String getRabbitPassword() {
        return rabbitPassword;
    }

    public String getRabbitVirtualHost() {
        return rabbitVirtualHost;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getWorldSettingsPath() {
        return worldSettingsPath;
    }

    public String getWorldId() {
        return worldId;
    }

    public static ServerConfig localDefault() {
        return fromArgs(new String[0]);
    }

    public static ServerConfig fromArgs(String[] args) {
        String host = env("CLIENTGAME_RABBIT_HOST", "localhost");
        int port = parseInt(env("CLIENTGAME_RABBIT_PORT", "5672"), 5672);
        String user = env("CLIENTGAME_RABBIT_USER", "guest");
        String password = env("CLIENTGAME_RABBIT_PASSWORD", "guest");
        String virtualHost = env("CLIENTGAME_RABBIT_VHOST", "/");
        String roomId = env("CLIENTGAME_ROOM_ID", "1");
        String worldSettingsPath = env("CLIENTGAME_WORLD_SETTINGS", "");
        String worldId = env("CLIENTGAME_WORLD_ID", "");

        if (args != null) {
            for (String arg : args) {
                if (arg == null || arg.isBlank()) {
                    continue;
                }
                if (arg.startsWith("--host=")) {
                    host = arg.substring("--host=".length());
                } else if (arg.startsWith("--port=")) {
                    port = parseInt(arg.substring("--port=".length()), port);
                } else if (arg.startsWith("--user=")) {
                    user = arg.substring("--user=".length());
                } else if (arg.startsWith("--password=")) {
                    password = arg.substring("--password=".length());
                } else if (arg.startsWith("--vhost=")) {
                    virtualHost = arg.substring("--vhost=".length());
                } else if (arg.startsWith("--room=")) {
                    roomId = arg.substring("--room=".length());
                } else if (arg.startsWith("--worldSettings=")) {
                    worldSettingsPath = arg.substring("--worldSettings=".length());
                } else if (arg.startsWith("--world=")) {
                    worldId = arg.substring("--world=".length());
                } else if (arg.startsWith("--worldId=")) {
                    worldId = arg.substring("--worldId=".length());
                }
            }
        }

        return new ServerConfig(host, port, user, password, virtualHost, roomId, worldSettingsPath, worldId);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return defaultIfBlank(value, fallback);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
