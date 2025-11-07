package com.hivemq.httpmqttbridge.brokerconfig.service;

import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBroker;
import com.hivemq.httpmqttbridge.brokerconfig.entity.MqttBrokerEntity;
import com.hivemq.httpmqttbridge.brokerconfig.mapper.MqttBrokerConfigurationMapper;
import com.hivemq.httpmqttbridge.brokerconfig.repository.MqttBrokerRepository;
import com.hivemq.httpmqttbridge.brokerconfig.request.MqttBrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.exception.MqttBrokerNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for handling Broker configuration requests.
 */

@Service
@RequiredArgsConstructor
public class MqttBrokerService {

  private final MqttBrokerRepository mqttBrokerRepository;

  @Transactional
  public MqttBroker saveOrUpdateBrokerConfiguration(
      MqttBrokerConfigurationRequest mqttBrokerConfigurationRequest) {
    return mqttBrokerRepository.findByHostNameAndPort(mqttBrokerConfigurationRequest.hostName(),
            mqttBrokerConfigurationRequest.port()).map(MqttBrokerConfigurationMapper::toMqttBroker)
        .orElseGet(() -> {
          MqttBrokerEntity mqttBrokerEntity = mqttBrokerRepository.save(
              MqttBrokerConfigurationMapper.toBrokerConfigurationEntity(
                  mqttBrokerConfigurationRequest));
          return MqttBrokerConfigurationMapper.toMqttBroker(mqttBrokerEntity);
        });
  }

  // Add Spring cache in future if needed
  public Optional<MqttBroker> getBrokerByBrokerId(Long brokerId) {
    Optional<MqttBrokerEntity> brokerConfigurationEntity = mqttBrokerRepository.findById(brokerId);
    return brokerConfigurationEntity.map(MqttBrokerConfigurationMapper::toMqttBroker);
  }

  public List<MqttBroker> getAllBrokers() {
    return mqttBrokerRepository.findAll().stream().map(MqttBrokerConfigurationMapper::toMqttBroker)
        .toList();
  }

  public void deleteBrokerById(Long brokerId) {
    mqttBrokerRepository.findById(brokerId).ifPresentOrElse(brokerEntity -> {
      mqttBrokerRepository.delete(brokerEntity);
      //For future -  Publish event to evict from cache clients.
    }, () -> {
      throw new MqttBrokerNotFoundException(brokerId);
    });
  }
  public void deleteAllBrokers() {
    mqttBrokerRepository.deleteAll();
  }
}
