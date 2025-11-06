package com.hivemq.httpmqttbridge.unit.consumer.controller;

import com.hivemq.httpmqttbridge.consumer.controller.MqttConsumerController;
import com.hivemq.httpmqttbridge.consumer.service.HiveMqttConsumerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MqttConsumerController.class)
public class MqttConsumerControllerTest {

    private static final String BROKER_ID = "1";
    private static final String TEST_TOPIC = "test-topic";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HiveMqttConsumerService consumerService;

    @Test
    void stream_whenTopicValid_startsSSEStream() throws Exception {
        doNothing().when(consumerService).stream(eq(BROKER_ID), eq(TEST_TOPIC), any(SseEmitter.class));

        mockMvc.perform(get("/mqtt/{brokerId}/receive/{topic}", BROKER_ID, TEST_TOPIC))
                .andExpect(request().asyncStarted())
                .andReturn();

        ArgumentCaptor<SseEmitter> emitterCaptor = ArgumentCaptor.forClass(SseEmitter.class);
        verify(consumerService).stream(eq(BROKER_ID), eq(TEST_TOPIC), emitterCaptor.capture());

        SseEmitter emitter = emitterCaptor.getValue();
        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void stream_whenServiceThrowsException_startsAsyncRequest() throws Exception {
        RuntimeException exception = new RuntimeException("Connection failed");
        doThrow(exception).when(consumerService).stream(eq(BROKER_ID), eq(TEST_TOPIC), any(SseEmitter.class));

        mockMvc.perform(get("/mqtt/{brokerId}/receive/{topic}", BROKER_ID, TEST_TOPIC))
                .andExpect(request().asyncStarted());

        verify(consumerService).stream(eq(BROKER_ID), eq(TEST_TOPIC), any(SseEmitter.class));
    }

}
