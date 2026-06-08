package com.tim.game.server.net;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.DeliverCallback;
import com.tim.game.shared.DTOs.update.MapInitDto;
import com.tim.game.shared.DTOs.update.MapChunkDto;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.MessageType;
import com.tim.game.shared.messaging.Topics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerMessageBus {

    private final ServerRabbitConnection connection;
    private final BlockingQueue<CommandMessage> commandQueue = new LinkedBlockingQueue<>();
    private final ObjectMapper objectMapper;

    public ServerMessageBus(ServerRabbitConnection connection) {
        this.connection = connection;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void startConsumingCommands() throws IOException {
        var channel = connection.getChannel();
        String queueName = connection.getCommandQueueName();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] body = delivery.getBody();

            try {
                CommandMessage command = deserializeCommand(body);
                commandQueue.offer(command);

                System.out.println("[Server] Received command: type=" + command.getType()
                        + " room=" + command.getRoomId()
                        + " client=" + command.getClientId());
            } catch (Exception e) {
                System.err.println("[Server] Failed to deserialize command:");
                e.printStackTrace();
            }
        };

        CancelCallback cancelCallback = consumerTag ->
                System.out.println("Command consumer cancelled: " + consumerTag);

        channel.basicConsume(queueName, true, deliverCallback, cancelCallback);
    }

    public List<CommandMessage> pollCommands() {
        List<CommandMessage> result = new ArrayList<>();
        commandQueue.drainTo(result);
        return result;
    }

    public void sendEvent(EventMessage event) {
        try {
            var channel = connection.getChannel();
            byte[] body = serializeEvent(event);

            String routingKey = event.getTargetClientId() != null
                    ? Topics.clientPrivate(event.getRoomId(), event.getTargetClientId())
                    : Topics.roomBroadcast(event.getRoomId());

            channel.basicPublish(Topics.EXCHANGE_UPDATES, routingKey, null, body);
        } catch (Exception e) {
            System.err.println("[Server] Failed to send event=" + event);
            e.printStackTrace();
        }
    }

    public void sendMapInitToClient(String roomId, String clientId, MapInitDto mapInit) {
        try {
            String payloadJson = objectMapper.writeValueAsString(mapInit);

            EventMessage event = new EventMessage(
                    MessageType.MAP_INIT,
                    roomId,
                    clientId,
                    payloadJson
            );

            sendEvent(event);
        } catch (Exception e) {
            System.err.println("[Server] Failed to send MAP_INIT to client=" + clientId + " room=" + roomId);
            e.printStackTrace();
        }
    }

    public void sendMapChunkToClient(String roomId, String clientId, MapChunkDto mapChunk) {
        try {
            String payloadJson = objectMapper.writeValueAsString(mapChunk);

            EventMessage event = new EventMessage(
                    MessageType.MAP_CHUNK,
                    roomId,
                    clientId,
                    payloadJson
            );

            sendEvent(event);
        } catch (Exception e) {
            System.err.println("[Server] Failed to send MAP_CHUNK to client=" + clientId + " room=" + roomId);
            e.printStackTrace();
        }
    }

    public void broadcastRoomState(EventMessage worldSnapshotEvent) {
        sendEvent(worldSnapshotEvent);
    }

    private CommandMessage deserializeCommand(byte[] body) throws IOException {
        return objectMapper.readValue(body, CommandMessage.class);
    }

    private byte[] serializeEvent(EventMessage event) throws IOException {
        return objectMapper.writeValueAsBytes(event);
    }
}
