package com.hivemq.httpmqttbridge.brokerconfig.controller;

import com.hivemq.httpmqttbridge.brokerconfig.domain.Broker;
import com.hivemq.httpmqttbridge.brokerconfig.request.BrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.brokerconfig.response.BrokerConfigurationResponse;
import com.hivemq.httpmqttbridge.brokerconfig.response.BrokerResponse;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import com.hivemq.httpmqttbridge.exception.BrokerNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mqtt/")
@Validated
@Slf4j
public class BrokerController {

  private final BrokerService brokerService;

  @PutMapping
  public ResponseEntity<BrokerConfigurationResponse> saveOrUpdateBrokerConfiguration(
      @RequestBody @Valid BrokerConfigurationRequest brokerConfigurationRequest) {
    Long brokerId = brokerService.saveOrUpdateBrokerConfiguration(brokerConfigurationRequest);
    return ResponseEntity.ok(BrokerConfigurationResponse.builder().brokerId(brokerId).build());
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{brokerId}")
  public ResponseEntity<BrokerResponse> getBrokerConfiguration(@PathVariable Long brokerId) {
    return brokerService
        .getBrokerByBrokerId(brokerId)
        .map(broker -> ResponseEntity.ok(BrokerResponse.fromBroker(broker)))
        .orElseThrow(() -> new BrokerNotFoundException(brokerId));
  }

  @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/{brokerId}")
  public ResponseEntity<BrokerResponse> deleteBrokerConfiguration(@PathVariable Long brokerId) {
    brokerService.deleteBrokerById(brokerId);
    return ResponseEntity.noContent().build();
  }
}
