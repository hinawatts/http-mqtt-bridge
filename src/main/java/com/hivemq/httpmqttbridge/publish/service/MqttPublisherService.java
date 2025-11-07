package com.hivemq.httpmqttbridge.publish.service;

import java.util.concurrent.CompletableFuture;

public interface MqttPublisherService {

  CompletableFuture<Void> publish(Long brokerId, String topic, Object payload, String requestId);
}
