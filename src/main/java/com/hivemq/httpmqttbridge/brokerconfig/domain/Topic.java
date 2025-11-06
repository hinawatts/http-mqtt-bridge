package com.hivemq.httpmqttbridge.brokerconfig.domain;

public record Topic(String topic, int qos, boolean retention) {
}
