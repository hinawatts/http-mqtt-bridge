package com.hivemq.httpmqttbridge.external.client.hivemq;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.httpmqttbridge.config.MqttProperties;
import com.hivemq.httpmqttbridge.brokerconfig.domain.Broker;
import com.hivemq.httpmqttbridge.brokerconfig.domain.BrokerCredentials;
import com.hivemq.httpmqttbridge.external.client.BrokerClientProvider;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import com.hivemq.httpmqttbridge.exception.MqttPublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.hivemq.httpmqttbridge.config.MqttProperties.DEFAULT_BROKER_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HiveMqttClientProvider implements BrokerClientProvider<Mqtt5AsyncClient> {

  private final BrokerService brokerService;
  private final ConcurrentMap<String, Mqtt5AsyncClient> clients = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, CompletableFuture<Mqtt5AsyncClient>> connects =
      new ConcurrentHashMap<>();
  private final MqttProperties mqttProperties;

  private static Mqtt5AsyncClient build(Broker broker) {
    Mqtt5ClientBuilder mqtt5ClientBuilder =
        Mqtt5Client.builder()
            .identifier("http-bridge-" + UUID.randomUUID())
            .serverHost(broker.hostName())
            .serverPort(broker.port())
            .automaticReconnect()
            .initialDelay(1, TimeUnit.SECONDS)
            .maxDelay(30, TimeUnit.SECONDS)
            .applyAutomaticReconnect()
            .addDisconnectedListener(
                ctx -> {
                  log.warn(
                      "Disconnected from broker (host={} port={}), reason={}",
                      broker.hostName(),
                      broker.port(),
                      ctx.getCause().getMessage());
                  ctx.getReconnector().reconnect(true);
                });
    boolean tls = broker.port() == 8883 || broker.port() == 443;
    if (tls) {
      mqtt5ClientBuilder = mqtt5ClientBuilder.sslWithDefaultConfig();
    }
    return mqtt5ClientBuilder.buildAsync();
  }

  @Override
  public CompletableFuture<Mqtt5AsyncClient> getClient(String brokerId) {
      log.info("Getting client for broker id {}", brokerId);

      // 1) Resolve broker config
      Optional<Broker> opt = brokerService.getBrokerByBrokerId(Long.parseLong(brokerId));
      if (opt.isEmpty()) {
          var failed = new CompletableFuture<Mqtt5AsyncClient>();
          failed.completeExceptionally(
                  new IllegalArgumentException("No broker config found for ID: " + brokerId));
          return failed;
      }
      Broker broker = opt.get();

      // 2) Fast-path: already have a connected client -> return immediately, don't touch 'connects'
      Mqtt5AsyncClient existing = clients.get(brokerId);
      if (existing != null && existing.getState().isConnected()) {
          log.info("Already connected to broker {} (host={} port={})",
                  brokerId, broker.hostName(), broker.port());
          return CompletableFuture.completedFuture(existing);
      }

      // 3) If there is an in-flight connect, reuse it
      CompletableFuture<Mqtt5AsyncClient> inflight = connects.get(brokerId);
      if (inflight != null) {
          return inflight;
      }

      // 4) Start a new connect attempt (single-flight)
      CompletableFuture<Mqtt5AsyncClient> start = new CompletableFuture<>();
      CompletableFuture<Mqtt5AsyncClient> previous = connects.putIfAbsent(brokerId, start);
      if (previous != null) {
          // Another thread beat us; use that one
          return previous;
      }

      // Build or reuse client instance
      Mqtt5AsyncClient client = clients.computeIfAbsent(brokerId, __ -> build(broker));

      log.info("Connecting to broker {} (host={} port={})", brokerId, broker.hostName(), broker.port());
      BrokerCredentials creds = mqttProperties.getBrokers()
              .getOrDefault(brokerId, mqttProperties.getBrokers().get(DEFAULT_BROKER_ID));

      client.connectWith()
              .cleanStart(true)
              .simpleAuth()
              .username(creds.userName())
              .password(creds.password().getBytes(StandardCharsets.UTF_8))
              .applySimpleAuth()
              .send()
              .orTimeout(7, TimeUnit.SECONDS)
              .whenComplete((ack, ex) -> {
                  // Ensure we clean 'connects' on both success and failure
                  connects.remove(brokerId);

                  if (ex != null) {
                      log.error("Failed to connect to broker {}", brokerId, ex);
                      start.completeExceptionally(
                              new MqttPublishException("Exception connecting to broker ID " + brokerId + ": " + ex));
                      return;
                  }

                  log.info("Connected to broker {} (host={} port={})",
                          brokerId, broker.hostName(), broker.port());
                  // Cache connected client
                  clients.put(brokerId, client);
                  start.complete(client);
              });

      return start;

  }

  @Override
  public void evict(String brokerId) {}

  @Override
  public void evictAll() {}
}
