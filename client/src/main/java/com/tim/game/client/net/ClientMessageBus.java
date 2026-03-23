package com.tim.game.client.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.Topics;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientMessageBus {
    private final ObjectMapper mapper = new ObjectMapper();
    private final BlockingQueue<EventMessage> incomingEvents = new LinkedBlockingQueue<>();

    private Channel channel;
    private String updatesQueueName;

    public void init(Channel channel, ClientConfig cfg) throws Exception {
        this.channel = channel;

        // Updates queue (client private)
        updatesQueueName = "client.updates.room." + cfg.roomId + ".client." + cfg.clientId;
        channel.queueDeclare(updatesQueueName, false, false, true, null);

        // Bind broadcast updates (muss zu Topics.roomBroadcast passen)
        channel.queueBind( updatesQueueName, Topics.EXCHANGE_UPDATES, Topics.roomBroadcast(cfg.roomId) );

        // Bind targeted updates (optional, muss zu Topics.clientPrivate passen)
        channel.queueBind( updatesQueueName, Topics.EXCHANGE_UPDATES, Topics.clientPrivate(cfg.roomId, cfg.clientId));

    }

    public void startConsumingUpdates() throws Exception {
        DeliverCallback deliver = (tag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
            EventMessage msg = mapper.readValue(json, EventMessage.class);
            incomingEvents.offer(msg);
            System.out.println("[Client] Update received: type=" + msg.getType()
        + " room=" + msg.getRoomId()
        + " payloadLen=" + (msg.getPayloadJson() != null ? msg.getPayloadJson().length() : 0));

        };
        channel.basicConsume(updatesQueueName, true, deliver, tag -> {});
    }

    public EventMessage pollEvent() {
        return incomingEvents.poll();
    }

    public void publishCommand(ClientConfig cfg, CommandMessage cmd) throws Exception {
        String routingKey = "room." + cfg.roomId + ".client." + cfg.clientId + ".input";
        String json = mapper.writeValueAsString(cmd);
        System.out.println("[Client] publish MOVE -> exchange=" + Topics.EXCHANGE_INPUTS
        + " rk=" + routingKey);

        channel.basicPublish(Topics.EXCHANGE_INPUTS, routingKey, null, json.getBytes(StandardCharsets.UTF_8));
    }
}
