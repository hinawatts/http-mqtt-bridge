package com.hivemq.httpmqttbridge.integration.brokerconfig.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.httpmqttbridge.brokerconfig.repository.BrokerRepository;
import com.hivemq.httpmqttbridge.brokerconfig.request.BrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.integration.setup.HttpMqttBridgeBaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BrokerControllerTest extends HttpMqttBridgeBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrokerRepository brokerRepository;

    private static final String BASE_URL = "/mqtt/";
    private static final Long BROKER_ID =  1l;
    private static final String BROKER_HOST_NAME = "test.host";
    private static final Integer BROKER_PORT = 1883;

    @BeforeEach
    void setUp() {
        brokerRepository.deleteAll(); // clean the DB before each test
    }
    @Test
    void shouldReturnNotFoundWhenNoBrokerExists() throws Exception {
        mockMvc.perform(get("/mqtt/"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSaveAndReturnBrokerConfiguration() throws Exception {
        // Arrange
        BrokerConfigurationRequest request = BrokerConfigurationRequest.builder()
                .hostName(BROKER_HOST_NAME)
                .port(BROKER_PORT)
                .build();

        // Act: Save broker
        mockMvc.perform(put("/mqtt/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brokerId").isNumber())
                .andExpect(jsonPath("$.brokerId", is(1)));

        // Assert: Retrieve broker
        mockMvc.perform(get("/mqtt/")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostName", is(BROKER_HOST_NAME)))
                .andExpect(jsonPath("$.port", is(BROKER_PORT)));
    }

    @Test
    void shouldUpdateAndReturn () throws Exception{
        // Arrange
        BrokerConfigurationRequest initialRequest = BrokerConfigurationRequest.builder()
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
        BrokerConfigurationRequest updateRequest = BrokerConfigurationRequest.builder()
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
                .andExpect(jsonPath("$.hostName", is(BROKER_HOST_NAME)))
                .andExpect(jsonPath("$.port", is(updatedPort)));
    }

}
