package com.hivemq.httpmqttbridge.brokerconfig.domain;

import lombok.Builder;

@Builder
public record MqttBroker(Long brokerId, String hostName, Integer port) {

}



