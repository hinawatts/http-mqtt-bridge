package com.hivemq.httpmqttbridge.unit.publisher.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.httpmqttbridge.brokerconfig.domain.Broker;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import com.hivemq.httpmqttbridge.exception.MqttPublishInputException;
import com.hivemq.httpmqttbridge.external.client.BrokerClientProvider;
import com.hivemq.httpmqttbridge.publisher.controller.MqttPublishController;
import com.hivemq.httpmqttbridge.publisher.service.MqttPublisher;
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
  private static final String TEST_HOST = "test.host.name";
  private static final int TEST_PORT = 1234;
  private static final Long BROKER_ID = 1L;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MqttPublisher hiveMqttPublisher;

  @Test
  void publishMessage_whenBrokerExists_andPublishSucceeds_returns202WithJson() throws Exception {

    // publisher completes normally
    Mockito.when(hiveMqttPublisher.publish(eq(String.valueOf(BROKER_ID)), eq(TEST_TOPIC), any()))
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
        .andExpect(status().isAccepted())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is("published")))
        .andExpect(jsonPath("$.brokerId", is(1)))
        .andExpect(jsonPath("$.topic", is(TEST_TOPIC)));
  }

  @Test
  void publishMessage_whenBrokerMissing_returns404() throws Exception {
    CompletableFuture<Void> pendingFuture = new CompletableFuture<>();
    Mockito.when(hiveMqttPublisher.publish(eq(String.valueOf(BROKER_ID)), eq(TEST_TOPIC), any()))
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
        .andExpect(jsonPath("$.message").value("Invalid Broker: Broker not found"));
  }
}
