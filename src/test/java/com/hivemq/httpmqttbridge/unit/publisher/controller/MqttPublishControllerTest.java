package com.hivemq.httpmqttbridge.unit.publisher.controller;

import com.hivemq.httpmqttbridge.exception.MqttPublishInputException;
import com.hivemq.httpmqttbridge.publish.controller.MqttPublishController;
import com.hivemq.httpmqttbridge.publish.service.MqttPublisherService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MqttPublishController.class)
public class MqttPublishControllerTest {

  private static final String TEST_TOPIC = "test-topic";
  private static final Long BROKER_ID = 1L;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MqttPublisherService hiveMqttPublisherService;

  @Test
  void publishMessage_whenBrokerExists_andPublishSucceeds_returns202WithJson() throws Exception {

    // publisher completes normally
    Mockito.when(hiveMqttPublisherService.publish(eq(BROKER_ID), eq(TEST_TOPIC), any(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    String bodyJson =
        """
            {"msg":"hello"}
        """;

    MvcResult pending =
        mockMvc
            .perform(
                post("/mqtt/{brokerId}/send/{topic}", BROKER_ID, TEST_TOPIC)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-request-id", "req-123")
                    .content(bodyJson))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc
        .perform(asyncDispatch(pending))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is("published")))
        .andExpect(jsonPath("$.brokerId", is(1)))
        .andExpect(jsonPath("$.topic", is(TEST_TOPIC)));
  }

  @Test
  void publishMessage_whenBrokerMissing_returns404() throws Exception {
    CompletableFuture<Void> pendingFuture = new CompletableFuture<>();
    Mockito.when(hiveMqttPublisherService.publish(eq(BROKER_ID), eq(TEST_TOPIC), any(),any()))
        .thenReturn(pendingFuture);

    MvcResult pending =
        mockMvc
            .perform(
                post("/mqtt/{brokerId}/send/{topic}", BROKER_ID, TEST_TOPIC)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"msg\":\"hello\"}"))
            .andExpect(request().asyncStarted())
            .andReturn();

    pendingFuture.completeExceptionally(
        new MqttPublishInputException("Invalid Broker: Broker not found"));
    mockMvc
        .perform(asyncDispatch(pending))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.failureReason").isNotEmpty());
  }
}
