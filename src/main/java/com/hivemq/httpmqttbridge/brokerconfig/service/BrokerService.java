package com.hivemq.httpmqttbridge.brokerconfig.service;

import com.hivemq.httpmqttbridge.brokerconfig.domain.Broker;
import com.hivemq.httpmqttbridge.brokerconfig.entity.BrokerEntity;
import com.hivemq.httpmqttbridge.brokerconfig.mapper.BrokerConfigurationMapper;
import com.hivemq.httpmqttbridge.brokerconfig.repository.BrokerRepository;
import com.hivemq.httpmqttbridge.brokerconfig.request.BrokerConfigurationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrokerService {

  private final BrokerRepository brokerRepository;

  public Long saveOrUpdateBrokerConfiguration(
      BrokerConfigurationRequest brokerConfigurationRequest) {
    return brokerRepository
        .findByHostName(brokerConfigurationRequest.getHostName())
        .map(
            existing -> {
              existing.setPort(brokerConfigurationRequest.getPort());
              // set other updatable fields here
              return brokerRepository.save(existing).getId(); // update
            })
        .orElseGet(
            () ->
                brokerRepository
                    .save(
                        BrokerConfigurationMapper.toBrokerConfigurationEntity(
                            brokerConfigurationRequest))
                    .getId()); // insert
  }

  public Optional<Broker> getBrokerByBrokerId(Long brokerId) {
    Optional<BrokerEntity> brokerConfigurationEntity = brokerRepository.findById(brokerId);
    return brokerConfigurationEntity.map(
        entity -> new Broker(entity.getId(), entity.getHostName(), entity.getPort()));
  }

  public List<Broker> getAllBrokers() {
    return brokerRepository.findAll().stream()
        .map(entity -> new Broker(entity.getId(), entity.getHostName(), entity.getPort()))
        .toList();
  }

  public void deleteBrokerById(Long brokerId) {
    brokerRepository.deleteById(brokerId);
  }
}
