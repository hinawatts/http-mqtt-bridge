package com.hivemq.httpmqttbridge.integration.consumer;

import com.hivemq.httpmqttbridge.consumer.service.HiveMqttConsumerService;
import com.hivemq.httpmqttbridge.integration.setup.HttpMqttBridgeBaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class MqttConsumerControllerTest extends HttpMqttBridgeBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HiveMqttConsumerService consumerService;

    private static final String BROKER_ID = "1";
    private static final String TOPIC = "test-topic";

    @Test
    void shouldStartSseStreamSuccessfully() throws Exception {
        Mockito.doAnswer(invocation -> {
            SseEmitter emitter = invocation.getArgument(2);
            emitter.send(SseEmitter.event().name("message").data("test message"));
            emitter.complete();
            return null;
        }).when(consumerService).stream(eq(BROKER_ID), eq(TOPIC), any(SseEmitter.class));

        mockMvc.perform(get("/mqtt/{brokerId}/receive/{topic}", BROKER_ID, TOPIC)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("test message")));
    }

    @Test
    void shouldReturnErrorEventWhenStreamingFails() throws Exception {
        Mockito.doAnswer(invocation -> {
            throw new RuntimeException("MQTT subscription failed");
        }).when(consumerService).stream(eq(BROKER_ID), eq(TOPIC), any(SseEmitter.class));

        mockMvc.perform(get("/mqtt/{brokerId}/receive/{topic}", BROKER_ID, TOPIC)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk()) // SSE still returns 200, even on emitter error
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Failed to start stream")));
    }

}
