package com.tim.game.server.net;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.tim.game.server.ServerConfig;
import com.tim.game.shared.messaging.Topics;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ServerRabbitConnection {
    private final ServerConfig config;
    private Connection connection;
    private Channel channel;
    
    // Name der Comman queues des rooms
    private String commandQueueName;

    public ServerRabbitConnection (ServerConfig config){
        this.config = config;
    }

    /**
     * Stellt die Verbindung zu RabbitMQ her und initialisiert Channel und Topologie.
     * Diese Methode baut einmal die Verbindung und den Channel auf und ruft danach declareTopology() auf, um Exchanges & Queue einzurichten.
        Warum ConnectionFactory?
        ConnectionFactory kapselt alle Verbindungsdaten:
            Host (localhost)
            Port (5672)
            User/Passwort
            Virtual Host
            newConnection() macht daraus eine echte TCP-Verbindung zum Broker.
     */
    public void connect() throws IOException, TimeoutException {
        //einrichtung der verbindung
        ConnectionFactory factory =  new ConnectionFactory();
        factory.setHost(config.getRabbitHost());
        factory.setPort(config.getRabbitPort());
        factory.setUsername(config.getRabbitUsername());
        factory.setPassword(config.getRabbitPassword());
        factory.setVirtualHost(config.getRabbitVirtualHost());

        this.connection =  factory.newConnection();
        this.channel =  connection.createChannel();

        declareTopology();
    }

    /**
     * Exchanges deklarieren: game.inputs "Topic"(alles was vom client kommt), game.updates"Topic"(alles was vom server kommt)
     * Queue: server.commands.room.<roomId> → pro Room eine Queue mit allen Client-Commands
     * Binding: alle Client-Inputs für diesen Raum
     *      Routing-Key:room.<roomId>.client.*.input → egal welcher Client (*), Hauptsache im richtigen Room, mit .input am Ende.
     */
    public void declareTopology()throws IOException{
        //exchange deklarieren
        channel.exchangeDeclare(Topics.EXCHANGE_INPUTS, "topic",true);
        channel.exchangeDeclare(Topics.EXCHANGE_UPDATES, "topic",true);

        // Commands-Queue für diesen Room
        this.commandQueueName = "server.commands.room" + config.getRoomId();
        channel.queueDeclare(commandQueueName, true, false,false, null);

        //Binding: alle client inputs für den raum
        String  routingKeyPattern = "room" + config.getRoomId() + ".client.*.input";
        channel.queueBind(commandQueueName, Topics.EXCHANGE_INPUTS,routingKeyPattern);
    }

        public Channel getChannel() {
        return channel;
    }

    public String getCommandQueueName() {
        return commandQueueName;
    }

    /**
     * Schließt Channel und Connection (wird beim Shutdown des Servers aufgerufen).
     */
    public void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception e) {
            // später durch ordentliches Logging ersetzen
            e.printStackTrace();
        }
    }

}