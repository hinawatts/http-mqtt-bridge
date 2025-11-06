package com.hivemq.httpmqttbridge.unit.publisher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.httpmqttbridge.brokerconfig.domain.Broker;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import com.hivemq.httpmqttbridge.external.client.BrokerClientProvider;
import com.hivemq.httpmqttbridge.publisher.service.HiveMqttPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.Files.readString;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = HiveMqttPublisherServiceTest.class)
public class HiveMqttPublisherServiceTest {

    @MockitoBean
    BrokerClientProvider<Mqtt5AsyncClient> provider;

    @MockitoBean
    private  BrokerService brokerService;

    @MockitoBean
    private ObjectMapper objectMapper;

    @Captor
    ArgumentCaptor<Mqtt5Publish> publishCaptor;

    @MockitoBean
    Mqtt5AsyncClient client;

    HiveMqttPublisherService publisher;

    @BeforeEach
    void setUp() {
        publisher = new HiveMqttPublisherService(provider, objectMapper, brokerService);
        ReflectionTestUtils.setField(publisher, "qos", 1);
        ReflectionTestUtils.setField(publisher, "retain", true);
    }

    @Test
    void publish_success_buildsMessageAndCompletes() throws Exception {
        String brokerId = "1";
        String topic = "test/topic";
        var payload = java.util.Map.of("msg", "hello");

        when(brokerService.getBrokerByBrokerId(1L))
                .thenReturn(Optional.of(new Broker(1L,"h",1883)));
        when(objectMapper.writeValueAsBytes(payload)).thenReturn("{\"msg\":\"hello\"}".getBytes());
        when(provider.getClient(brokerId))
                .thenReturn(CompletableFuture.completedFuture(client));
        when(client.publish(any(Mqtt5Publish.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> publishFuture = publisher.publish(brokerId, topic, payload);
        assertThatCode(publishFuture::join).doesNotThrowAnyException();

        verify(client).publish(publishCaptor.capture());
        Mqtt5Publish sent = publishCaptor.getValue();
        assertThat(sent.getTopic().toString()).isEqualTo(topic);
        assertThat(sent.getQos()).isEqualTo(MqttQos.AT_LEAST_ONCE); // qos=1
        assertThat(sent.isRetain()).isTrue();
        verify(provider).getClient("1");
    }

}
