package com.hivemq.httpmqttbridge.external.client.hivemq;

import static com.hivemq.httpmqttbridge.config.MqttProperties.DEFAULT_BROKER_ID;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBroker;
import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBrokerCredentials;
import com.hivemq.httpmqttbridge.brokerconfig.service.MqttBrokerService;
import com.hivemq.httpmqttbridge.config.MqttProperties;
import com.hivemq.httpmqttbridge.exception.MqttBrokerNotFoundException;
import com.hivemq.httpmqttbridge.exception.MqttPublishException;
import com.hivemq.httpmqttbridge.external.client.MqttBrokerClientProvider;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Client Provider for HiveMQ MQTT brokers. Creates client per broker and manages their
 * connections.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class HiveMqttClientProvider implements MqttBrokerClientProvider<Mqtt5AsyncClient> {

  private final MqttBrokerService mqttBrokerService;
  private final ConcurrentMap<Long, Mqtt5AsyncClient> clients = new ConcurrentHashMap<>();
  private final ConcurrentMap<Long, CompletableFuture<Mqtt5AsyncClient>> connects = new ConcurrentHashMap<>();
  private final MqttProperties mqttProperties;

  private static Mqtt5AsyncClient build(MqttBroker mqttBroker) {
    Mqtt5ClientBuilder mqtt5ClientBuilder = Mqtt5Client.builder()
        .identifier("http-bridge-" + UUID.randomUUID()).serverHost(mqttBroker.hostName())
        .serverPort(mqttBroker.port()).automaticReconnect().initialDelay(1, TimeUnit.SECONDS)
        .maxDelay(30, TimeUnit.SECONDS).applyAutomaticReconnect().addDisconnectedListener(ctx -> {
          log.warn("Disconnected from broker (host={} port={}), reason={}", mqttBroker.hostName(),
              mqttBroker.port(), ctx.getCause().getMessage());
          ctx.getReconnector().reconnect(true);
        });
    boolean tls = mqttBroker.port() == 8883 || mqttBroker.port() == 443;
    if (tls) {
      mqtt5ClientBuilder = mqtt5ClientBuilder.sslWithDefaultConfig();
    }
    return mqtt5ClientBuilder.buildAsync();
  }

  @Override
  public CompletableFuture<Mqtt5AsyncClient> getClient(Long brokerId) {
    Mqtt5AsyncClient existing = clients.get(brokerId);
    if (existing != null && existing.getState().isConnected()) {
      return CompletableFuture.completedFuture(existing);
    }
    CompletableFuture<Mqtt5AsyncClient> inflight = connects.get(brokerId);
    if (inflight != null) {
      return inflight;
    }
    Optional<MqttBroker> mqttBrokerOptional = mqttBrokerService.getBrokerByBrokerId(brokerId);
    if (mqttBrokerOptional.isEmpty()) {
      var failed = new CompletableFuture<Mqtt5AsyncClient>();
      failed.completeExceptionally(new MqttBrokerNotFoundException(brokerId));
      return failed;
    }
    MqttBroker mqttBroker = mqttBrokerOptional.get();

    CompletableFuture<Mqtt5AsyncClient> start = new CompletableFuture<>();
    CompletableFuture<Mqtt5AsyncClient> previous = connects.putIfAbsent(brokerId, start);
    if (previous != null) {
      return previous;
    }

    Mqtt5AsyncClient client = clients.computeIfAbsent(brokerId, id -> build(mqttBroker));

    MqttBrokerCredentials credentials = mqttProperties.getBrokers()
        .getOrDefault(brokerId, mqttProperties.getBrokers().get(DEFAULT_BROKER_ID));
    connectClient(brokerId, client, credentials, start, mqttBroker);
    return start;

  }

  private void connectClient(Long brokerId, Mqtt5AsyncClient client,
      MqttBrokerCredentials credentials, CompletableFuture<Mqtt5AsyncClient> start,
      MqttBroker mqttBroker) {
    client.connectWith().cleanStart(true).simpleAuth().username(credentials.userName())
        .password(credentials.password().getBytes(StandardCharsets.UTF_8)).applySimpleAuth().send()
        .orTimeout(7, TimeUnit.SECONDS).whenComplete((ack, ex) -> {
          connects.remove(brokerId);
          if (ex != null) {
            log.error("Failed to connect to broker {}", brokerId, ex);
            start.completeExceptionally(
                new MqttPublishException("Exception connecting to broker ID " + brokerId, ex));
            return;
          }

          log.info("Connected to broker {} (host={} port={})", brokerId, mqttBroker.hostName(),
              mqttBroker.port());
          clients.put(brokerId, client);
          start.complete(client);
        });
  }

  @Override
  public void evict(Long brokerId) {
    CompletableFuture<Mqtt5AsyncClient> inflight = connects.remove(brokerId);
    if (inflight != null) {
      inflight.cancel(true);
    }
    Mqtt5AsyncClient client = clients.remove(brokerId);
    if (client != null) {
      try {
        client.disconnect();
      } catch (Exception e) {
        log.warn("Error disconnecting client for brokerId={}: {}", brokerId, e.getMessage());
      }
    }
  }

}
