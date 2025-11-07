package com.hivemq.httpmqttbridge.external.client;

import java.util.concurrent.CompletableFuture;

public interface MqttBrokerClientProvider<T> {

  CompletableFuture<T> getClient(Long brokerId);

  void evict(Long brokerId);

}
