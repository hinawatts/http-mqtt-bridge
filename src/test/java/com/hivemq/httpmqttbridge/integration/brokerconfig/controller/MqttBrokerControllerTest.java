package com.hivemq.httpmqttbridge.integration.brokerconfig.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.httpmqttbridge.brokerconfig.repository.MqttBrokerRepository;
import com.hivemq.httpmqttbridge.brokerconfig.request.MqttBrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.integration.setup.HttpMqttBridgeBaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class MqttBrokerControllerTest extends HttpMqttBridgeBaseIntegrationTest {

  private static final String BASE_URL = "/mqtt/";
  private static final String BROKER_HOST_NAME = "test.host";
  private static final Integer BROKER_PORT = 1883;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private MqttBrokerRepository mqttBrokerRepository;

  @BeforeEach
  void setUp() {
    mqttBrokerRepository.deleteAll(); // clean the DB before each test
  }

  @Test
  void shouldReturnEmptyListWhenNoBrokerExists() throws Exception {

    mockMvc.perform(get(BASE_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

  }

  @Test
  void shouldSaveNewBrokerAndReturnBrokerId() throws Exception {

    MqttBrokerConfigurationRequest request = MqttBrokerConfigurationRequest.builder()
        .hostName(BROKER_HOST_NAME)
        .port(BROKER_PORT)
        .build();

    // Act: Save broker
    mockMvc.perform(put("/mqtt/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brokerId").isNotEmpty());

    // Assert: Retrieve broker
    mockMvc.perform(get("/mqtt/")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void shouldUpdateAndReturn() throws Exception {
    // Arrange
    MqttBrokerConfigurationRequest initialRequest = MqttBrokerConfigurationRequest.builder()
        .hostName(BROKER_HOST_NAME)
        .port(BROKER_PORT)
        .build();

    // Save initial broker
    mockMvc.perform(put("/mqtt/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(initialRequest))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brokerId").isNumber());

    // Create update request;
    Integer updatedPort = 8883;
    MqttBrokerConfigurationRequest updateRequest = MqttBrokerConfigurationRequest.builder()
        .hostName(BROKER_HOST_NAME)
        .port(updatedPort)
        .build();

    //Update broker
    mockMvc.perform(put("/mqtt/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brokerId").isNumber());

    // Assert: Retrieve updated broker
    mockMvc.perform(get("/mqtt/")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  void shouldGetBrokerById_whenBrokerExists_returnsOk() throws Exception {
    MqttBrokerConfigurationRequest request = MqttBrokerConfigurationRequest.builder()
        .hostName(BROKER_HOST_NAME)
        .port(BROKER_PORT)
        .build();

    // Save broker
    String responseContent = mockMvc.perform(put("/mqtt/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long savedBrokerId = objectMapper.readTree(responseContent).get("brokerId").asLong();

    // Act & Assert: Retrieve broker by ID
    mockMvc.perform(get("/mqtt/{brokerId}/", savedBrokerId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brokerId", is(savedBrokerId.intValue())))
        .andExpect(jsonPath("$.hostName", is(BROKER_HOST_NAME)))
        .andExpect(jsonPath("$.port", is(BROKER_PORT)));
  }
}
