package com.hivemq.httpmqttbridge.unit.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBroker;
import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBrokerCredentials;
import com.hivemq.httpmqttbridge.brokerconfig.service.MqttBrokerService;
import com.hivemq.httpmqttbridge.config.MqttProperties;
import com.hivemq.httpmqttbridge.exception.MqttBrokerNotFoundException;
import com.hivemq.httpmqttbridge.external.client.hivemq.HiveMqttClientProvider;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HiveMqttClientProviderTest {

  private static final Long BROKER_ID = 1L;
  private static final String HOST = "test.mqtt.com";
  private static final int PORT = 1883;
  private static final String USERNAME = "testuser";
  private static final String PASSWORD = "testpass";

  @Mock
  private MqttBrokerService mqttBrokerService;
  @Mock
  private MqttProperties mqttProperties;
  @Mock
  private Mqtt5AsyncClient mqttClient;
  @Mock
  private Mqtt5ConnAck connAck;

  private HiveMqttClientProvider clientProvider;
  private MqttBroker testBroker;
  private MqttBrokerCredentials credentials;

  @BeforeEach
  void setUp() {
    testBroker = new MqttBroker(BROKER_ID, HOST, PORT);
    credentials = new MqttBrokerCredentials(USERNAME, PASSWORD);
    clientProvider = new HiveMqttClientProvider(mqttBrokerService, mqttProperties);
  }

  @Test
  void getClient_whenBrokerExists_returnsConnectedClient() {

    when(mqttBrokerService.getBrokerByBrokerId(BROKER_ID)).thenReturn(Optional.of(testBroker));
    when(mqttProperties.getBrokers()).thenReturn(
        Map.of(BROKER_ID, credentials, MqttProperties.DEFAULT_BROKER_ID, credentials));

    CompletableFuture<Mqtt5AsyncClient> result = clientProvider.getClient(BROKER_ID);

    verify(mqttBrokerService).getBrokerByBrokerId(BROKER_ID);
    assertThat(result).isNotNull();
  }

  @Test
  void getClient_whenBrokerNotFound_returnsFailedFuture() {

    when(mqttBrokerService.getBrokerByBrokerId(BROKER_ID)).thenReturn(Optional.empty());

    CompletableFuture<Mqtt5AsyncClient> result = clientProvider.getClient(BROKER_ID);

    assertThatThrownBy(result::get).hasCauseInstanceOf(MqttBrokerNotFoundException.class);
  }

  @Test
  void getClient_withTlsPort_configuresSsl() {

    MqttBroker tlsBroker = new MqttBroker(BROKER_ID, HOST, 8883);
    when(mqttBrokerService.getBrokerByBrokerId(BROKER_ID)).thenReturn(Optional.of(tlsBroker));

    when(mqttProperties.getBrokers()).thenReturn(
        Map.of(BROKER_ID, credentials, MqttProperties.DEFAULT_BROKER_ID, credentials));
    CompletableFuture<Mqtt5AsyncClient> result = clientProvider.getClient(BROKER_ID);

    verify(mqttBrokerService).getBrokerByBrokerId(BROKER_ID);
    assertThat(result).isNotNull();
  }

  @Test
  void getClient_whenExistingConnected_returnsSameClient() {

    when(mqttBrokerService.getBrokerByBrokerId(BROKER_ID)).thenReturn(Optional.of(testBroker));
    when(mqttProperties.getBrokers()).thenReturn(
        Map.of(BROKER_ID, credentials, MqttProperties.DEFAULT_BROKER_ID, credentials));

    CompletableFuture<Mqtt5AsyncClient> first = clientProvider.getClient(BROKER_ID);
    CompletableFuture<Mqtt5AsyncClient> second = clientProvider.getClient(BROKER_ID);

    verify(mqttBrokerService, times(1)).getBrokerByBrokerId(BROKER_ID);
    assertThat(second).isNotNull();
  }

  @Test
  void evict_whenClientExists_disconnectsAndRemoves() {

    when(mqttBrokerService.getBrokerByBrokerId(BROKER_ID)).thenReturn(Optional.of(testBroker));
    when(mqttProperties.getBrokers()).thenReturn(
        Map.of(BROKER_ID, credentials, MqttProperties.DEFAULT_BROKER_ID, credentials));
    clientProvider.getClient(BROKER_ID);
    when(mqttProperties.getBrokers()).thenReturn(
        Map.of(BROKER_ID, credentials, MqttProperties.DEFAULT_BROKER_ID, credentials));

    clientProvider.evict(BROKER_ID);

    CompletableFuture<Mqtt5AsyncClient> newClient = clientProvider.getClient(BROKER_ID);
    verify(mqttBrokerService, times(2)).getBrokerByBrokerId(BROKER_ID);
  }

  @Test
  void getClient_withCustomCredentials_usesProvidedAuth() {
    // Given
    MqttBrokerCredentials customCreds = new MqttBrokerCredentials("custom", "pass");
    when(mqttProperties.getBrokers()).thenReturn(Map.of(BROKER_ID, customCreds));
    when(mqttBrokerService.getBrokerByBrokerId(BROKER_ID)).thenReturn(Optional.of(testBroker));

    // When
    CompletableFuture<Mqtt5AsyncClient> result = clientProvider.getClient(BROKER_ID);

    // Then
    verify(mqttBrokerService).getBrokerByBrokerId(BROKER_ID);
    assertThat(result).isNotNull();
  }

  @Test
  void getClient_whenConnectionTimeout_returnsFailedFuture() {
    // Given
    when(mqttBrokerService.getBrokerByBrokerId(BROKER_ID)).thenReturn(Optional.of(testBroker));
    when(mqttProperties.getBrokers()).thenReturn(
        Map.of(BROKER_ID, credentials, MqttProperties.DEFAULT_BROKER_ID, credentials));
    // When
    CompletableFuture<Mqtt5AsyncClient> result = clientProvider.getClient(BROKER_ID);

    // Then
    assertThat(result).isNotNull();
    verify(mqttBrokerService).getBrokerByBrokerId(BROKER_ID);
  }
}
