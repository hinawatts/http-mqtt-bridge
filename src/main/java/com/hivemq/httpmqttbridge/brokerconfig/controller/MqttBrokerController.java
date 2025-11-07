package com.hivemq.httpmqttbridge.brokerconfig.controller;

import com.hivemq.httpmqttbridge.brokerconfig.request.MqttBrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.brokerconfig.response.MqttBrokerResponse;
import com.hivemq.httpmqttbridge.brokerconfig.service.MqttBrokerService;
import com.hivemq.httpmqttbridge.exception.MqttBrokerNotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing MQTT broker configurations.
 * Provides endpoints to create, retrieve, and delete broker configurations.
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/mqtt")
@Validated
@Slf4j
public class MqttBrokerController {

  private final MqttBrokerService mqttBrokerService;

  @PutMapping(path = "/")
  @Tag(name = "MQTT Broker Configuration", description = "API to create or update MQTT broker configurations")
  public MqttBrokerResponse saveOrUpdateBrokerConfiguration(
      @RequestBody @Valid MqttBrokerConfigurationRequest mqttBrokerConfigurationRequest) {

    return MqttBrokerResponse.fromBroker(
        mqttBrokerService.saveOrUpdateBrokerConfiguration(mqttBrokerConfigurationRequest));
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{brokerId}/")
  @Tag(name = "MQTT Broker Configuration", description = "API to get MQTT broker configuration by broker ID")
  public MqttBrokerResponse getBrokerById(@PathVariable Long brokerId) {
    return mqttBrokerService.getBrokerByBrokerId(brokerId)
        .map(MqttBrokerResponse::fromBroker).orElseThrow(() -> {
          log.error("GetBrokerByIdApi - Broker {} not found", brokerId);
          return new MqttBrokerNotFoundException(brokerId);
        });
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/")
  @Tag(name = "MQTT Broker Configuration", description = "API to get all MQTT broker configurations")
  public List<MqttBrokerResponse> getAllBrokers() {
    return mqttBrokerService.getAllBrokers().stream().map(MqttBrokerResponse::fromBroker).toList();
  }

  @DeleteMapping(path = "/{brokerId}/")
  @Tag(name = "MQTT Broker Configuration", description = "API to delete MQTT broker configuration by broker ID")
  public void deleteBrokerById(@PathVariable Long brokerId) {
    mqttBrokerService.deleteBrokerById(brokerId);
  }
}
