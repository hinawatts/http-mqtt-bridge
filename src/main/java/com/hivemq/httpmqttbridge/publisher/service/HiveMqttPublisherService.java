package com.hivemq.httpmqttbridge.publisher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import com.hivemq.httpmqttbridge.exception.MqttPublishException;
import com.hivemq.httpmqttbridge.exception.MqttPublishInputException;
import com.hivemq.httpmqttbridge.external.client.BrokerClientProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class HiveMqttPublisherService implements MqttPublisher {

  private final BrokerClientProvider<Mqtt5AsyncClient> provider;

  private final ObjectMapper objectMapper;

  private final BrokerService brokerService;

  @Value("${mqtt.publisher.qos}")
  private int qos;

  @Value("${mqtt.publisher.retain}")
  private boolean retain;

  @Override
  public CompletableFuture<Void> publish(String brokerId, String topic, Object payload) {

    byte[] payloadBytes = getPayloadBytes(payload);
    validateBroker(brokerId);
    log.info("Publishing {} to {}", topic, payloadBytes);
    return provider
        .getClient(brokerId)
        .thenCompose(
            client ->
                client.publish(
                    Mqtt5Publish.builder()
                        .topic(topic)
                        .qos(getQos(qos))
                        .retain(retain)
                        .payload(ByteBuffer.wrap(payloadBytes))
                        .build()))
        .thenAccept(__ -> {});
  }

  private void validateBroker(String brokerId) {
    brokerService
        .getBrokerByBrokerId(Long.parseLong(brokerId))
        .orElseThrow(() -> new MqttPublishInputException("Invalid Broker: Broker not found"));
  }

  private byte[] getPayloadBytes(Object payload) {
    byte[] payloadBytes;
    try {
      payloadBytes = objectMapper.writeValueAsBytes(payload);
    } catch (Exception e) {
      throw new MqttPublishException("Failed to serialize payload: " + e.getMessage());
    }
    return payloadBytes;
  }

  private MqttQos getQos(int qos) {
    if (MqttQos.fromCode(qos) == null) {
      return MqttQos.AT_MOST_ONCE;
    }
    return MqttQos.fromCode(qos);
  }
}
