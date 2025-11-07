package com.hivemq.httpmqttbridge.exception;

public class MqttBrokerNotFoundException extends RuntimeException {

  public MqttBrokerNotFoundException(Long brokerId) {
    super("Broker with ID " + brokerId + " not found.");
  }
}
