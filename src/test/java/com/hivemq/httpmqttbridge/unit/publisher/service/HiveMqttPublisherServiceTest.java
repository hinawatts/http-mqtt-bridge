package com.hivemq.httpmqttbridge.unit.publisher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.httpmqttbridge.external.client.MqttBrokerClientProvider;
import com.hivemq.httpmqttbridge.publish.service.HiveMqttPublisherService;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HiveMqttPublisherServiceTest {

  @Mock
  MqttBrokerClientProvider<Mqtt5AsyncClient> provider;

  @Mock
  Mqtt5AsyncClient client;

  @Mock
  ObjectMapper objectMapper;

  @Captor
  ArgumentCaptor<Mqtt5Publish> publishCaptor;

  private HiveMqttPublisherService publisher;

  @BeforeEach
  void setUp() {
    publisher = new HiveMqttPublisherService(provider, objectMapper);
    // set private @Value fields used by the publisher
    ReflectionTestUtils.setField(publisher, "qos", 1);     // maps to AT_LEAST_ONCE
    ReflectionTestUtils.setField(publisher, "retain", true);
  }

  @Test
  void publish_success_buildsMessageAndCompletes() throws Exception {
    // Arrange
    Long brokerId = 1L;
    String topic = "test/topic";
    Map<String, Object> payload = Map.of("msg", "hello");

    when(objectMapper.writeValueAsBytes(payload))
        .thenReturn("{\"msg\":\"hello\"}".getBytes());

    when(provider.getClient(brokerId))
        .thenReturn(CompletableFuture.completedFuture(client));

    // HiveMQ async client returns a future when publishing; we complete it successfully
    when(client.publish(any(Mqtt5Publish.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    // Act
    var publishFuture = publisher.publish(brokerId, topic, payload, "req-1");

    // Assert
    assertThatCode(publishFuture::join).doesNotThrowAnyException();

    verify(client).publish(publishCaptor.capture());
    Mqtt5Publish sent = publishCaptor.getValue();

    assertThat(sent.getTopic().toString()).isEqualTo(topic);
    assertThat(sent.getQos()).isEqualTo(MqttQos.AT_LEAST_ONCE); // qos=1
    assertThat(sent.isRetain()).isTrue();

    verify(provider).getClient(brokerId);
  }
}
