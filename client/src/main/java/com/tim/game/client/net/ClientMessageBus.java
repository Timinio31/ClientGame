package com.tim.game.client.net;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.tim.game.shared.messaging.CommandMessage;
import com.tim.game.shared.messaging.EventMessage;
import com.tim.game.shared.messaging.Topics;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientMessageBus {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final BlockingQueue<EventMessage> incomingEvents = new LinkedBlockingQueue<>();

    private Channel channel;
    private String updatesQueueName;

    public void init(Channel channel, ClientConfig cfg) throws Exception {
        this.channel = channel;

        updatesQueueName = "client.updates.room." + cfg.roomId + ".client." + cfg.clientId;
        channel.queueDeclare(updatesQueueName, false, false, true, null);

        channel.queueBind(updatesQueueName, Topics.EXCHANGE_UPDATES, Topics.roomBroadcast(cfg.roomId));
        channel.queueBind(updatesQueueName, Topics.EXCHANGE_UPDATES, Topics.clientPrivate(cfg.roomId, cfg.clientId));

        System.out.println("[Client] Update queue=" + updatesQueueName);
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

        channel.basicConsume(updatesQueueName, true, deliver, tag -> {
        });
    }

    public EventMessage pollEvent() {
        return incomingEvents.poll();
    }

    public void publishCommand(ClientConfig cfg, CommandMessage cmd) throws Exception {
        String routingKey = Topics.clientInput(cfg.roomId, cfg.clientId);
        String json = mapper.writeValueAsString(cmd);

        System.out.println("[Client] publish " + cmd.getType()
                + " -> exchange=" + Topics.EXCHANGE_INPUTS
                + " rk=" + routingKey);

        channel.basicPublish(Topics.EXCHANGE_INPUTS, routingKey, null, json.getBytes(StandardCharsets.UTF_8));
    }
}
