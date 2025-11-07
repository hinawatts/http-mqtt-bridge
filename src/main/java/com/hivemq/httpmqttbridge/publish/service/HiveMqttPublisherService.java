package com.hivemq.httpmqttbridge.publish.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.httpmqttbridge.exception.MqttBrokerNotFoundException;
import com.hivemq.httpmqttbridge.exception.MqttPublishException;
import com.hivemq.httpmqttbridge.exception.MqttPublishInputException;
import com.hivemq.httpmqttbridge.external.client.MqttBrokerClientProvider;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publish service to handle connecting and publishing messages to HiveMq MQTT brokers.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class HiveMqttPublisherService implements MqttPublisherService {

  private final MqttBrokerClientProvider<Mqtt5AsyncClient> mqttBrokerClientProvider;

  private final ObjectMapper objectMapper;

  @Value("${mqtt.publisher.qos}")
  private int qos;

  @Value("${mqtt.publisher.retain}")
  private boolean retain;

  @Override
  public CompletableFuture<Void> publish(Long brokerId, String topic, Object payload,
      String requestId) {

    //validateBroker(brokerId, requestId);
    byte[] payloadBytes = getPayloadBytes(payload, requestId);

    return mqttBrokerClientProvider.getClient(brokerId).thenCompose(client -> client.publish(
        Mqtt5Publish.builder().topic(topic).qos(getQos(qos)).retain(retain)
            .payload(ByteBuffer.wrap(payloadBytes)).build())).thenAccept(publishResult -> {
      log.debug("Successfully published requestId - {} to broker {} on topic {}", requestId,
          brokerId, topic);
    }).exceptionally(ex -> {
      log.error("Failed to publish to broker {} on topic {}: {}", brokerId, topic, ex.getMessage(),
          ex);
      if (ex.getCause() != null && ex.getCause() instanceof MqttBrokerNotFoundException) {
        throw new MqttPublishInputException(ex.getMessage());
      }

      throw new MqttPublishException(ex.getMessage());
    });
  }

/*  private  CompletableFuture<Void> validateBroker(Long brokerId, String requestId) {
    mqttBrokerService.getBrokerByBrokerId(brokerId).orElseThrow(() -> {
      log.error("MqttPublishRequest - Broker with id {} not found for request - {} ", brokerId,
          requestId);
      return CompletableFuture.failedFuture(new BrokerNotFoundException(brokerId));
    });
  }*/

  private byte[] getPayloadBytes(Object payload, String requestId) {
    byte[] payloadBytes;
    try {
      payloadBytes = objectMapper.writeValueAsBytes(payload);
    } catch (Exception e) {
      log.error("MqttPublishRequest - Failed to serialize payload: {} -  for request - {} ",
          e.getMessage(), requestId);
      throw new MqttPublishException("Failed to serialize payload: " + e.getMessage());
    }
    return payloadBytes;
  }

  private MqttQos getQos(int qos) {
    MqttQos q = MqttQos.fromCode(qos);
    return q != null ? q : MqttQos.AT_LEAST_ONCE;
  }
}
