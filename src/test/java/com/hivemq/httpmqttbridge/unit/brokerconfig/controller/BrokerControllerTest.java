package com.hivemq.httpmqttbridge.unit.brokerconfig.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.httpmqttbridge.brokerconfig.controller.BrokerController;
import com.hivemq.httpmqttbridge.brokerconfig.request.BrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BrokerController.class)
@AutoConfigureMockMvc
public class BrokerControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BrokerService brokerService;

  private static Stream<Arguments> getInvalidBrokerConfigurationRequests() {
    return Stream.of(
        Arguments.of(
            "empty hostname", BrokerConfigurationRequest.builder().hostName("").port(1883).build()),
        Arguments.of(
            "negative port",
            BrokerConfigurationRequest.builder().hostName("validHost").port(-1).build()),
        Arguments.of(
            "port exceeds maximum",
            BrokerConfigurationRequest.builder().hostName("validHost").port(70000).build()));
  }

  @Test
  void getBrokerConfiguration_whenEmpty_returnsNotFound() throws Exception {
    when(brokerService.getAllBrokers()).thenReturn(List.of());
    mockMvc.perform(get("/mqtt/")).andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @MethodSource("getInvalidBrokerConfigurationRequests")
  void saveOrUpdateBroker_invalidRequest(
      String testName, BrokerConfigurationRequest brokerConfigurationRequest) throws Exception {
    mockMvc
        .perform(
            put("/mqtt/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(brokerConfigurationRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").exists());
  }

    @Test
    void saveOrUpdateBrokerConfiguration_whenValidRequest_returnsOkWithBrokerId() throws Exception {
        BrokerConfigurationRequest request = BrokerConfigurationRequest.builder()
                .hostName("localhost")
                .port(1883)
                .build();
        Long expectedBrokerId = 1L;

        when(brokerService.saveOrUpdateBrokerConfiguration(request)).thenReturn(expectedBrokerId);

        mockMvc.perform(put("/mqtt/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brokerId").value(expectedBrokerId));
    }


}
