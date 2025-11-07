package com.hivemq.httpmqttbridge.brokerconfig.response;

import lombok.Builder;

@Builder
public record MqttBrokerConfigurationResponse(Long brokerId) {

}
