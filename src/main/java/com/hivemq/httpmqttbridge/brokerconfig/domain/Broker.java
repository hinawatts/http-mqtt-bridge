package com.hivemq.httpmqttbridge.brokerconfig.domain;

import lombok.Builder;

@Builder
public record Broker (Long brokerId, String hostName, Integer port) {

}



