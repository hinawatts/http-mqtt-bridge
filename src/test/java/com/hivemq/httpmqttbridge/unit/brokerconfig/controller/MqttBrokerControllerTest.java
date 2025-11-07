package com.hivemq.httpmqttbridge.unit.brokerconfig.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.httpmqttbridge.brokerconfig.controller.MqttBrokerController;
import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBroker;
import com.hivemq.httpmqttbridge.brokerconfig.request.MqttBrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.brokerconfig.service.MqttBrokerService;
import com.hivemq.httpmqttbridge.exception.MqttBrokerNotFoundException;
import java.util.List;
import java.util.Optional;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(MqttBrokerController.class)
@AutoConfigureMockMvc
public class MqttBrokerControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MqttBrokerService mqttBrokerService;

  private static Stream<Arguments> getInvalidBrokerConfigurationRequests() {
    return Stream.of(Arguments.of("empty hostname",
            MqttBrokerConfigurationRequest.builder().hostName("").port(1883).build()),
        Arguments.of("negative port",
            MqttBrokerConfigurationRequest.builder().hostName("validHost").port(-1).build()),
        Arguments.of("port exceeds maximum",
            MqttBrokerConfigurationRequest.builder().hostName("validHost").port(70000).build()));
  }

  @Test
  void getBrokerConfiguration_whenEmpty_returnsNotFound() throws Exception {
    when(mqttBrokerService.getAllBrokers()).thenReturn(List.of());
    mockMvc.perform(get("/mqtt/")).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @ParameterizedTest
  @MethodSource("getInvalidBrokerConfigurationRequests")
  void saveOrUpdateBroker_invalidRequest(String testName,
      MqttBrokerConfigurationRequest mqttBrokerConfigurationRequest) throws Exception {
    mockMvc.perform(put("/mqtt/").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mqttBrokerConfigurationRequest)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
  }

  @Test
  void saveOrUpdateBrokerConfiguration_whenValidRequest_returnsOkWithBrokerId() throws Exception {
    MqttBrokerConfigurationRequest request = MqttBrokerConfigurationRequest.builder()
        .hostName("localhost").port(1883).build();
    MqttBroker expectedBroker = MqttBroker.builder()
        .brokerId(1L)
        .hostName("localhost")
        .port(1883)
        .build();

    when(mqttBrokerService.saveOrUpdateBrokerConfiguration(request))
        .thenReturn(expectedBroker);

    mockMvc.perform(put("/mqtt/").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.brokerId").value(expectedBroker.brokerId()));
  }

  @Test
  void deleteBrokerById_whenBrokerExists_returnsNoContent() throws Exception {
    Long brokerId = 1L;

    mockMvc.perform(MockMvcRequestBuilders
            .delete("/mqtt/{brokerId}/", brokerId))
        .andExpect(status().isOk());
  }

  @Test
  void deleteBrokerById_whenBrokerDoesNotExist_returns404() throws Exception {
    long brokerId = 1L;
    doThrow(new MqttBrokerNotFoundException(brokerId)).when(mqttBrokerService).deleteBrokerById(brokerId);

    mockMvc.perform(MockMvcRequestBuilders
            .delete("/mqtt/{brokerId}/", brokerId))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldGetBrokerById_whenBrokerExists_returnsOk() throws Exception {
    Long brokerId = 1L;
    String hostName = "test.host";
    Integer port = 1883;

    var broker = MqttBroker.builder()
        .brokerId(brokerId)
        .hostName(hostName)
        .port(port)
        .build();

    when(mqttBrokerService.getBrokerByBrokerId(brokerId))
        .thenReturn(Optional.of(broker));

    mockMvc.perform(get("/mqtt/{brokerId}/", brokerId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.brokerId").value(brokerId))
        .andExpect(jsonPath("$.hostName").value(hostName))
        .andExpect(jsonPath("$.port").value(port));
  }

}
