package com.hivemq.httpmqttbridge.external.client;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.util.concurrent.CompletableFuture;

public interface BrokerClientProvider<T> {

  CompletableFuture<T> getClient(String brokerId);

  void evict(String brokerId); // call after deleting/updating a broker config

  void evictAll();
}
