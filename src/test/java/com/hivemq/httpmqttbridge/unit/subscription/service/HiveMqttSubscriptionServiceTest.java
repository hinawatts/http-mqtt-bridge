
package com.hivemq.httpmqttbridge.unit.subscription.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.httpmqttbridge.external.client.MqttBrokerClientProvider;
import com.hivemq.httpmqttbridge.subscribe.service.HiveMqttSubscriptionService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class HiveMqttSubscriptionServiceTest {

  private static final String TEST_TOPIC = "test-topic";

  @Captor
  ArgumentCaptor<Consumer<Mqtt5Publish>> callbackCaptor;

  @Mock private MqttBrokerClientProvider<Mqtt5AsyncClient> clientProvider;
  @Mock private Mqtt5AsyncClient mqttClient;
  @Mock private SseEmitter emitter;
  @InjectMocks private HiveMqttSubscriptionService consumerService;

  private static final Long BROKER_ID = 1L;

  @Test
  void shouldHandleClientAcquisitionFailure() throws Exception {
    when(clientProvider.getClient(BROKER_ID))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Broker not found")));

    consumerService.stream(BROKER_ID, TEST_TOPIC, emitter);
      verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
      verify(emitter).completeWithError(any());
  }
    @Test
    void shouldSetupSubscriptionWhenClientIsAcquired() {
        var subscribeStart = mock(Mqtt5AsyncClient.Mqtt5SubscribeAndCallbackBuilder.Start.class);
        var subscribeComplete = mock(Mqtt5AsyncClient.Mqtt5SubscribeAndCallbackBuilder.Start.Complete.class);
        var callback = mock(Mqtt5AsyncClient.Mqtt5SubscribeAndCallbackBuilder.Call.Ex.class);

        when(clientProvider.getClient(BROKER_ID)).thenReturn(CompletableFuture.completedFuture(mqttClient));
        when(mqttClient.subscribeWith()).thenReturn(subscribeStart);
        when(subscribeStart.topicFilter(anyString())).thenReturn(subscribeComplete);
        when(subscribeComplete.qos(any())).thenReturn(subscribeComplete);
        when(subscribeComplete.retainHandling(any())).thenReturn(subscribeComplete);
        when(subscribeComplete.callback(any())).thenReturn(callback);
        when(callback.send()).thenReturn(new CompletableFuture<>());

        consumerService.stream(BROKER_ID, TEST_TOPIC, emitter);
        verify(mqttClient, timeout(1000)).subscribeWith();
        verify(callback).send();
        verify(subscribeComplete).callback(callbackCaptor.capture());
        Consumer<Mqtt5Publish> captured = callbackCaptor.getValue();
        assertNotNull(captured);
    }
}
