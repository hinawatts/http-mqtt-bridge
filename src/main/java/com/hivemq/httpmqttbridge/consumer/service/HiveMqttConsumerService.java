package com.hivemq.httpmqttbridge.consumer.service;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.httpmqttbridge.external.client.BrokerClientProvider;
import com.hivemq.httpmqttbridge.publisher.service.MqttPublisher;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.hivemq.httpmqttbridge.consumer.ConsumerErrorHandler.sendSseError;

@Service
@RequiredArgsConstructor
@Slf4j
public class HiveMqttConsumerService implements MqttConsumerService {

  private final BrokerClientProvider<Mqtt5AsyncClient> clientProvider;

  private static void processMessage(String brokerId, SseEmitter emitter, Mqtt5Publish pub) {
    pub.getPayload()
        .ifPresent(
            buf -> {
              try {
                byte[] bytes = new byte[buf.remaining()];
                buf.get(bytes);
                // send UTF-8 text so Insomnia shows it clearly
                String text = new String(bytes, StandardCharsets.UTF_8);
                emitter.send(
                    SseEmitter.event().name("message").data(text, MediaType.APPLICATION_JSON));
              } catch (Exception sendEx) {
                log.error("Error sending SSE message for broker {}", brokerId, sendEx);
                sendSseError(
                    emitter, "SEND", "Failed to send SSE message: " + sendEx.getMessage(), sendEx);
              }
            });
  }

  @Override
  public void stream(String brokerId, String topic, SseEmitter emitter) {
    log.info("Streaming on broker id {} topic {}", brokerId, topic);
    clientProvider
        .getClient(brokerId)
        .handle(
            (client, ex) -> {
              if (ex != null) {
                log.error("MQTT client acquisition failed for broker {}", brokerId, ex);
                sendSseError(
                    emitter, "CLIENT_CONNECT", "Broker connection failed: " + ex.getMessage(), ex);
                  emitter.completeWithError(ex);
                return null; // <- required by handle()
              }
              subscribeAndStream(brokerId, topic, emitter, client);
              return null;
            });
  }

  private void subscribeAndStream(
      String brokerId, String topic, SseEmitter emitter, Mqtt5AsyncClient client) {
    var subscription =
        client
            .subscribeWith()
            .topicFilter(topic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .retainHandling(Mqtt5RetainHandling.SEND)
            .callback(pub -> processMessage(brokerId, emitter, pub))
            .send();

    setupEmitterLifecycle(emitter, brokerId, topic, client);
    handleSubscriptionResult(subscription, emitter, brokerId, topic, client);
    startHeartbeat(emitter, brokerId, topic);
  }

  private void setupEmitterLifecycle(
      SseEmitter emitter, String brokerId, String topic, Mqtt5AsyncClient client) {
    Runnable unsubscribe =
        () -> {
          try {
            client
                .unsubscribeWith()
                .topicFilter(topic)
                .send()
                .orTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .join();
          } catch (Exception ignored) {
          }
        };

    emitter.onCompletion(
        () -> {
          log.info("SSE completed for brokerId={} topic={}", brokerId, topic);
          unsubscribe.run();
        });

    emitter.onTimeout(
        () -> {
          log.warn("SSE timeout for brokerId={} topic={}", brokerId, topic);
          sendSseError(emitter, "TIMEOUT", "SSE timed out", null);
          unsubscribe.run();
        });

    emitter.onError(
        ioEx -> {
          log.warn("SSE I/O error for brokerId={} topic={}", brokerId, topic, ioEx);
          sendSseError(emitter, "EMITTER_IO", "SSE I/O error: " + ioEx.getMessage(), ioEx);
          unsubscribe.run();
        });
  }

  private void handleSubscriptionResult(
      CompletableFuture<Mqtt5SubAck> subscription,
      SseEmitter emitter,
      String brokerId,
      String topic,
      Mqtt5AsyncClient client) {

    subscription.whenComplete(
        (ok, subscriptionException) -> {
          if (subscriptionException != null) {
            log.error(
                "Subscribe failed brokerId={} topic={}", brokerId, topic, subscriptionException);
            sendSseError(
                emitter,
                "SUBSCRIBE",
                "Subscribe failed: " + subscriptionException.getMessage(),
                subscriptionException);
            unsubscribeQuietly(client, topic);
            return;
          }

          try {
            emitter.send(SseEmitter.event().name("subscribed").data("listening on " + topic));
          } catch (Exception sendEx) {
            log.error("Failed to send 'subscribed' event", sendEx);
            sendSseError(
                emitter,
                "SEND_SUB",
                "Failed to send subscribed event: " + sendEx.getMessage(),
                sendEx);
            unsubscribeQuietly(client, topic);
          }
        });
  }

  private void unsubscribeQuietly(Mqtt5AsyncClient client, String topic) {
    try {
      client
          .unsubscribeWith()
          .topicFilter(topic)
          .send()
          .orTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
          .join();
    } catch (Exception ignored) {
    }
  }

  private void startHeartbeat(SseEmitter emitter, String brokerId, String topic) {
    var scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        () -> {
          try {
            emitter.send(SseEmitter.event().comment("keep-alive"));
          } catch (Exception ex) {
            sendSseError(emitter, "HEARTBEAT", "Heartbeat failed: " + ex.getMessage(), ex);
          }
        },
        10,
        10,
        java.util.concurrent.TimeUnit.SECONDS);

    emitter.onCompletion(scheduler::shutdownNow);
    emitter.onTimeout(scheduler::shutdownNow);
  }

  public void unsubscribe(Mqtt3AsyncClient client, String topic) {
    try {
      client.unsubscribeWith().topicFilter(topic).send().join();
    } catch (Exception ignored) {
    }
  }
}
