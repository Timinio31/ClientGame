package com.tim.game.client.net;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.tim.game.shared.messaging.Topics;

public class ClientRabbitConnection {
    private Connection connection;
    private Channel channel;

    public void connect(ClientConfig cfg) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(cfg.rabbitHost);
        factory.setPort(cfg.rabbitPort);
        factory.setUsername(cfg.username);
        factory.setPassword(cfg.password);
        factory.setVirtualHost(cfg.virtualHost);

        connection = factory.newConnection("client-" + cfg.clientId);
        channel = connection.createChannel();
System.out.println("[Client] connect -> " + cfg.rabbitHost + ":" + cfg.rabbitPort
        + " user=" + cfg.username + " vhost=" + cfg.virtualHost);

        channel.exchangeDeclare(Topics.EXCHANGE_INPUTS, "topic", true);
        channel.exchangeDeclare(Topics.EXCHANGE_UPDATES, "topic", true);
    }


    public Channel channel(){
        return channel;
    }

    public void close(){
        try { 
            if (channel != null) channel.close(); 
        } catch (
            Exception ignored
        ) {}
        
        try { 
            if (connection != null) connection.close(); 
        } catch (
            Exception ignored
        ) {}
    }
}