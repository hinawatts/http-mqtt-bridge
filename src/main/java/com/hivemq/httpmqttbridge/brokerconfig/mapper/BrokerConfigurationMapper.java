package com.hivemq.httpmqttbridge.brokerconfig.mapper;


import com.hivemq.httpmqttbridge.brokerconfig.entity.BrokerEntity;
import com.hivemq.httpmqttbridge.brokerconfig.request.BrokerConfigurationRequest;

public class BrokerConfigurationMapper {

    public static BrokerEntity toBrokerConfigurationEntity(
            final BrokerConfigurationRequest brokerConfigurationRequest) {
        return BrokerEntity.builder()
                .hostName(brokerConfigurationRequest.getHostName())
                .port(brokerConfigurationRequest.getPort())
                .build();
    }
}
