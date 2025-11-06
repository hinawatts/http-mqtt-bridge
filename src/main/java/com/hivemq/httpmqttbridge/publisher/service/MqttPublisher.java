package com.hivemq.httpmqttbridge.publisher.service;

import java.util.concurrent.CompletableFuture;

public interface MqttPublisher {
  CompletableFuture<Void> publish(String brokerId, String topic, Object payload);
}
