package com.hivemq.httpmqttbridge.subscribe.service;

import static com.hivemq.httpmqttbridge.subscribe.MqttSubscriptionErrorHandler.sendSseError;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.httpmqttbridge.external.client.MqttBrokerClientProvider;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class HiveMqttSubscriptionService implements MqttSubscriptionService {

  private static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newScheduledThreadPool(
      1);
  private final MqttBrokerClientProvider<Mqtt5AsyncClient> clientProvider;
  private final SseEmitterHandler sseEmitterHandler;
  @Value("${mqtt.subscription.sse.heartbeat-interval-ms:10000}")
  private long heartbeatMs;
  @Value("${mqtt.subscription.mqtt.timeout-ms:5000}")
  private long subscribeTimeoutMs;


  private static void processMessage(Long brokerId, SseEmitter emitter, Mqtt5Publish pub) {
    pub.getPayload().ifPresent(buf -> {
      try {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        String text = new String(bytes, StandardCharsets.UTF_8);
        emitter.send(SseEmitter.event().name("message")
            .data(text, MediaType.APPLICATION_JSON));
      } catch (Exception sendEx) {
        log.error("Error sending SSE message for broker {}", brokerId, sendEx);
        sendSseError(emitter, "SEND", "Failed to send SSE message: " + sendEx.getMessage(), sendEx);
      }
    });
  }

  @Override
  public void stream(Long brokerId, String topic, SseEmitter emitter) {
    log.debug("Streaming on broker id - {} topic - {}", brokerId, topic);
    clientProvider.getClient(brokerId).handle((client, ex) -> {
      if (ex != null) {
        log.error("MQTT client acquisition failed for broker - {} , exception - {}", brokerId,
            ex.getMessage());
        sendSseError(emitter, "CLIENT_CONNECT", "Broker connection failed: " + ex.getMessage(), ex);
        emitter.completeWithError(ex);
        return null;
      }
      subscribeAndStream(brokerId, topic, emitter, client);
      return null;
    });
  }

  private void subscribeAndStream(Long brokerId, String topic, SseEmitter emitter,
      Mqtt5AsyncClient client) {
    var subscription = client.subscribeWith().topicFilter(topic).qos(MqttQos.AT_LEAST_ONCE)
        .retainHandling(Mqtt5RetainHandling.SEND)
        .callback(pub -> processMessage(brokerId, emitter, pub)).send()
        .orTimeout(subscribeTimeoutMs, TimeUnit.MILLISECONDS);

    setupEmitterLifecycle(emitter, brokerId, topic, client);
    handleSubscriptionResult(subscription, emitter, brokerId, topic, client);
    startHeartbeat(emitter);
  }

  private void setupEmitterLifecycle(SseEmitter emitter, Long brokerId, String topic,
      Mqtt5AsyncClient client) {
    Runnable unsubscribe = () -> {
      try {
        client.unsubscribeWith().topicFilter(topic).send().orTimeout(2000, TimeUnit.MILLISECONDS)
            .join();
      } catch (Exception exception) {
        log.error("Error unsubscribing from topic - {}", topic, exception);
      }
    };
    sseEmitterHandler.setupEmitterLifecycle(emitter, brokerId, topic, unsubscribe);
  }

  private void handleSubscriptionResult(CompletableFuture<Mqtt5SubAck> subscription,
      SseEmitter emitter, Long brokerId, String topic, Mqtt5AsyncClient client) {

    subscription.whenComplete((ok, subscriptionException) -> {
      if (subscriptionException != null) {
        log.error("Subscribe failed brokerId={} topic={}", brokerId, topic, subscriptionException);
        sendSseError(emitter, "SUBSCRIBE",
            "Subscribe failed: " + subscriptionException.getMessage(), subscriptionException);
        unsubscribe(client, topic);
        return;
      }
      try {
        emitter.send(SseEmitter.event().name("subscribed").data("listening on " + topic));
      } catch (Exception sendEx) {
        log.error("Failed to send 'subscribed' event", sendEx);
        sendSseError(emitter, "SEND_SUB", "Failed to send subscribed event: " + sendEx.getMessage(),
            sendEx);
        unsubscribe(client, topic);
      }
    });
  }

  private void unsubscribe(Mqtt5AsyncClient client, String topic) {
    try {
      client.unsubscribeWith().topicFilter(topic).send().orTimeout(2000, TimeUnit.MILLISECONDS)
          .join();
    } catch (Exception exception) {
      log.error("Failed to unsubscribe {} topic", topic, exception);
    }
  }

  private void startHeartbeat(SseEmitter emitter) {
    var sseHeartBeatFuture = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
      try {
        emitter.send(SseEmitter.event().comment("keep-alive"));
      } catch (Exception ex) {
        sendSseError(emitter, "HEARTBEAT", "Heartbeat failed: " + ex.getMessage(), ex);
        emitter.completeWithError(ex);
      }
    }, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);

    emitter.onCompletion(() -> sseHeartBeatFuture.cancel(true));
    emitter.onTimeout(() -> sseHeartBeatFuture.cancel(true));
  }
}
