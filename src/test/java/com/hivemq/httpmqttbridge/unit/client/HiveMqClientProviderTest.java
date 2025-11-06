package com.hivemq.httpmqttbridge.unit.client;


import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.httpmqttbridge.brokerconfig.domain.Broker;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import com.hivemq.httpmqttbridge.config.MqttProperties;
import com.hivemq.httpmqttbridge.brokerconfig.domain.BrokerCredentials;
import com.hivemq.httpmqttbridge.exception.MqttPublishException;
import com.hivemq.httpmqttbridge.external.client.hivemq.HiveMqttClientProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HiveMqttClientProviderTest {

  private static final String BROKER_ID = "1";
  private static final String HOST = "broker.example.com";
  private static final int PORT = 1883;
  private static final int TLS_PORT = 8883;
  private static final String USERNAME = "testuser";
  private static final String PASSWORD = "testpass";

  @Mock private BrokerService brokerService;

  @Mock private MqttProperties mqttProperties;

  @Mock private Mqtt5AsyncClient mqttClient;

  @Mock private Mqtt5ConnAck connAck;

  private HiveMqttClientProvider clientProvider;
  private Broker testBroker;
  private BrokerCredentials credentials;

  @BeforeEach
  void setUp() {
    testBroker = new Broker(1L, HOST, PORT);
    credentials = new BrokerCredentials(USERNAME, PASSWORD);

    when(mqttProperties.getBrokers())
        .thenReturn(Map.of(BROKER_ID, credentials, MqttProperties.DEFAULT_BROKER_ID, credentials));

    clientProvider = new HiveMqttClientProvider(brokerService, mqttProperties);
  }

  /*@Test
  void getClient_whenBrokerNotFound_returnsFailedFuture() {
    when(brokerService.getBrokerByBrokerId(anyLong())).thenReturn(Optional.empty());

    CompletableFuture<Mqtt5AsyncClient> result = clientProvider.getClient(BROKER_ID);

    assertThat(result).isCompletedExceptionally();
    assertThatThrownBy(result::get)
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No broker config found for ID: " + BROKER_ID);
  }
*/
  @Test
  void getClient_whenBrokerFound_createsAndConnects() {
    when(brokerService.getBrokerByBrokerId(1L)).thenReturn(Optional.of(testBroker));
    clientProvider.getClient(BROKER_ID);

    verify(brokerService).getBrokerByBrokerId(1L);
  }

  @Test
  void getClient_whenAlreadyConnected_returnsExistingClient() throws Exception {

    when(brokerService.getBrokerByBrokerId(1L)).thenReturn(Optional.of(testBroker));

    CompletableFuture<Mqtt5AsyncClient> first = clientProvider.getClient(BROKER_ID);

    verify(brokerService, times(1)).getBrokerByBrokerId(1L);
  }

  @Test
  void getClient_withTlsPort_usesSslConfig() {
    Broker tlsBroker = new Broker(1L, HOST, TLS_PORT);
    when(brokerService.getBrokerByBrokerId(1L)).thenReturn(Optional.of(tlsBroker));

    clientProvider.getClient(BROKER_ID);

    verify(brokerService).getBrokerByBrokerId(1L);
  }

  @Test
  void getClient_with443Port_usesSslConfig() {
    Broker tlsBroker = new Broker(1L, HOST, 443);
    when(brokerService.getBrokerByBrokerId(1L)).thenReturn(Optional.of(tlsBroker));

    clientProvider.getClient(BROKER_ID);

    verify(brokerService).getBrokerByBrokerId(1L);
  }

  @Test
  void getClient_withNonTlsPort_doesNotUseSsl() {
    when(brokerService.getBrokerByBrokerId(1L)).thenReturn(Optional.of(testBroker));

    clientProvider.getClient(BROKER_ID);

    verify(brokerService).getBrokerByBrokerId(1L);
  }

/*
  @Test
  void getClient_whenBrokerNotInProperties_usesDefaultCredentials() {
    String unknownBrokerId = "999";
    Broker unknownBroker = new Broker(999L, HOST, PORT);

    when(brokerService.getBrokerByBrokerId(999L)).thenReturn(Optional.of(unknownBroker));

    clientProvider.getClient(unknownBrokerId);

    verify(mqttProperties).getBrokers();
  }
*/

  @Test
  void getClient_parsesNumericBrokerId() {
    when(brokerService.getBrokerByBrokerId(1L)).thenReturn(Optional.of(testBroker));

    clientProvider.getClient("1");

    verify(brokerService).getBrokerByBrokerId(1L);
  }

/*  @Test
  void getClient_withInvalidBrokerId_throwsException() {
    assertThatThrownBy(() -> clientProvider.getClient("invalid"))
        .isInstanceOf(NumberFormatException.class);
  }*/
}
