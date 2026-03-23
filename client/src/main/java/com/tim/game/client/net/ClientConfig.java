package com.tim.game.client.net;

public class ClientConfig { 
    public final String rabbitHost;
    public final int rabbitPort;
    public final String username;
    public final String  password;
    public final String virtualHost;

    public final String roomId;
    public final String  clientId;

    private ClientConfig(String rabbitHost, int rabbitPort, String username, String password, String virtualHost, String roomId, String clientId){
        this.rabbitHost = rabbitHost;
        this.rabbitPort = rabbitPort;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;
        this.roomId = roomId;
        this.clientId = clientId;
    }

    public static ClientConfig localDefault(String clientId) {
            return new ClientConfig(
                "localhost",
                5672,
                "guest",
                "guest",
                "/",
                "1",
                clientId
        );
    }
}