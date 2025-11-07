package com.hivemq.httpmqttbridge.brokerconfig.mapper;


import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBroker;
import com.hivemq.httpmqttbridge.brokerconfig.entity.MqttBrokerEntity;
import com.hivemq.httpmqttbridge.brokerconfig.request.MqttBrokerConfigurationRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MqttBrokerConfigurationMapper {

  public MqttBrokerEntity toBrokerConfigurationEntity(
      final MqttBrokerConfigurationRequest mqttBrokerConfigurationRequest) {
    return MqttBrokerEntity.builder()
        .hostName(mqttBrokerConfigurationRequest.hostName())
        .port(mqttBrokerConfigurationRequest.port())
        .build();
  }

  public MqttBroker toMqttBroker(MqttBrokerEntity entity) {
    return MqttBroker.builder()
        .brokerId(entity.getId())
        .hostName(entity.getHostName())
        .port(entity.getPort())
        .build();
  }
}
