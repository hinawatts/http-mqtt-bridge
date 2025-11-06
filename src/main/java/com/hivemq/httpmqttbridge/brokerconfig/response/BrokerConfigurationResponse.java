package com.hivemq.httpmqttbridge.brokerconfig.response;

import lombok.Builder;

@Builder
public record BrokerConfigurationResponse(Long brokerId) {
}
