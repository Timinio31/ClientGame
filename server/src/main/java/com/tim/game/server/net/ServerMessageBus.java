package com.tim.game.server.net;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.DeliverCallback;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.Topics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class ServerMessageBus {
    
    //Verbindung + Channel + Queue-Name.
    private final ServerRabbitConnection connection;

    // Thread-sichere Queue für Commands aus RabbitMQ
    private final BlockingQueue<CommandMessage> commandQueue = new LinkedBlockingQueue<>();
    private final ObjectMapper objectMapper;

    public ServerMessageBus(ServerRabbitConnection connection) {
        this.connection = connection;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    /**Wir hängen uns mit einem Consumer an die Commands-Queue des Servers und legen jede eingehende Nachricht in unsere commandQueue.
     * Später: hier wird aus byte[] → CommandMessage deserialisiert (JSON, etc.).
     * Jetzt erstmal nur Struktur + TODO.
     * Warum DeliverCallback?
     *      RabbitMQ ruft diese Funktion jedes Mal auf, wenn eine Nachricht in der Queue landet.
     *      Wir lesen delivery.getBody() und machen damit, was wir wollen.
    */


   /**update wenn clients senden */
   public void startConsumingCommands() throws IOException{
    var channel = connection.getChannel();
    String queueName= connection.getCommandQueueName();

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        byte[] body = delivery.getBody();
        // TODO: hier body -> CommandMessage deserialisieren
            // Beispiel (später mit JSON):
            // CommandMessage cmd = deserializeCommand(body);
            // commandQueue.offer(cmd);
        
        // Für den Moment: nur debuggen
            System.out.println("Received raw command bytes, length=" + body.length);
    };

    CancelCallback cancelCallback = consumerTag -> {
        System.out.println("Command consumer cancelled: " + consumerTag);
    };

    //autoAck = true (fürs erste ausreichend)
    channel.basicConsume(queueName,true, deliverCallback,cancelCallback);
   }



    /**
     * Wird im GameLoop aufgerufen, um alle seit dem letzten Tick eingegangenen Commands abzuholen.
     */
    public List<CommandMessage> pollCommands() {
        List<CommandMessage> result = new ArrayList<>();
        //verschiebt alles aus commandQueue in eine Liste und leert sie.
        commandQueue.drainTo(result);
        return result;
    }

    //===============Server to client==============
    
    /**
     * Sendet ein Event (z.B. Entity-Update oder World-Snapshot) an die passenden Clients.
     */
    public void sendEvent(EventMessage event) {
        try {
            var channel = connection.getChannel();

            // TODO: EventMessage -> byte[] serialisieren (z.B. JSON)
            byte[] body = new byte[0];

            String routingKey;
            if (event.getTargetClientId() != null) {
                routingKey = Topics.clientPrivate(event.getRoomId(), event.getTargetClientId());
            } else {
                routingKey = Topics.roomBroadcast(event.getRoomId());
            }

            channel.basicPublish(Topics.EXCHANGE_UPDATES, routingKey, null, body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Komfort-Methode, um z.B. regelmäßig den kompletten Weltzustand zu broadcasten.
     */
    public void broadcastRoomState(EventMessage worldSnapshotEvent) {
        sendEvent(worldSnapshotEvent);
    }


    /*======== hilfsmethoden ========*/
    private CommandMessage deserializeCommand(byte[] body) throws IOException {
        return objectMapper.readValue(body, CommandMessage.class);
    }

    private byte[] serializeEvent(EventMessage event) throws IOException {
        return objectMapper.writeValueAsBytes(event);
    }

}
