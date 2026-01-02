package com.tim.game.server;


public class ServerConfig {
    /**
     * rabbitHost → wohin soll sich der Server verbinden? (bei dir: "localhost")
     * rabbitPort → Standard-RabbitMQ-Port: 5672
     * rabbitUsername / rabbitPassword → aktuell guest/guest (oder dein eigener User) 
     * rabbitVirtualHost → / oder z. B. clientgame, falls du einen eigenen VHost angelegt hast
     * roomId → brauchen wir für:
     *      Queue-Namen server.commands.room.<roomId>
     *      Routing-Keys room.<roomId>.client.*.input
     */
    private final String rabbitHost;
    private final int rabbitPort;
    private final String rabbitUsername;
    private final String rabbitPassword;
    private final String rabbitVirtualHost;

    private final String roomId;


    public ServerConfig(String rabbitHost, int rabbitPort,String rabbitUsername, String rabbitPassword, String rabbitVirtualHost, String roomId){
        this.rabbitHost = rabbitHost;
        this.rabbitPort = rabbitPort;
        this.rabbitUsername = rabbitUsername;
        this.rabbitPassword = rabbitPassword;
        this.rabbitVirtualHost = rabbitVirtualHost;
        this.roomId = roomId;
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

    public static ServerConfig localDefault() {
        return new ServerConfig(
                "localhost",
                5672,
                "guest",
                "guest",
                "/",
                "1"
        );
    }


}