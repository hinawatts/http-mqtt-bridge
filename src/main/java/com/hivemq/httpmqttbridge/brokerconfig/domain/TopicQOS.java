package com.hivemq.httpmqttbridge.brokerconfig.domain;

public enum TopicQOS {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2);

    private final int qosLevel;

    TopicQOS(int qosLevel) {
        this.qosLevel = qosLevel;
    }

    public int getQosLevel() {
        return qosLevel;
    }
}
