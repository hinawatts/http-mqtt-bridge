package com.hivemq.httpmqttbridge.exception;

public class BrokerNotFoundException extends  RuntimeException {

    public BrokerNotFoundException(Long brokerId) {
        super("Broker with ID " + brokerId + " not found.");
    }
}
