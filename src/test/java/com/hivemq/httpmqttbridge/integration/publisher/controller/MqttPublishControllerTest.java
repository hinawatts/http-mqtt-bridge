package com.hivemq.httpmqttbridge.integration.publisher.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.httpmqttbridge.brokerconfig.entity.BrokerEntity;
import com.hivemq.httpmqttbridge.brokerconfig.repository.BrokerRepository;
import com.hivemq.httpmqttbridge.exception.MqttPublishInputException;
import com.hivemq.httpmqttbridge.integration.setup.HttpMqttBridgeBaseIntegrationTest;
import com.hivemq.httpmqttbridge.publisher.service.MqttPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MqttPublishControllerTest extends HttpMqttBridgeBaseIntegrationTest {

    private static final String TEST_TOPIC = "test-topic";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BrokerRepository brokerRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private MqttPublisher mqttPublisher;
    private Long brokerId;

    @BeforeEach
    void setUp() {
        brokerRepository.deleteAll();

        BrokerEntity brokerEntity = brokerRepository.save(BrokerEntity.builder()
                .hostName("test.broker")
                .port(1883)
                .build());

        brokerId = brokerEntity.getId();

        // Mock successful publish

    }

    @Test
    void shouldPublishMessageAndReturnAccepted() throws Exception {
        Map<String, Object> messageBody = Map.of(
                "message", "hello mqtt",
                "qos", 1
        );
        Mockito.when(mqttPublisher.publish(eq(String.valueOf(brokerId)), eq(TEST_TOPIC), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        MvcResult pending = mockMvc.perform(post("/mqtt/{brokerId}/send/{topic}", brokerId, TEST_TOPIC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("x-request-id", "test-request-id")
                        .content(objectMapper.writeValueAsString(messageBody))).andReturn();
        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("published"))
                .andExpect(jsonPath("$.brokerId").value(brokerId))
                .andExpect(jsonPath("$.topic").value(TEST_TOPIC));
    }

    @Test
    void shouldReturn400WhenInputValidationFails() throws Exception {
        // Mock input validation exception
        Mockito.when(mqttPublisher.publish(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new MqttPublishInputException("Invalid input")));

        MvcResult pending =  mockMvc.perform(post("/mqtt/{brokerId}/send/{topic}", brokerId, TEST_TOPIC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{}")).andReturn();
        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Mqtt Publish Input Error"))
                .andExpect(jsonPath("$.message").value("Invalid input"));
    }

    @Test
    void shouldReturn500WhenUnhandledExceptionOccurs() throws Exception {
        Mockito.when(mqttPublisher.publish(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Publish Exception")));

        MvcResult pending  =  mockMvc.perform(post("/mqtt/{brokerId}/send/{topic}", brokerId, TEST_TOPIC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"test\"}")).andReturn();
        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isInternalServerError());
    }
}

