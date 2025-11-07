package com.hivemq.httpmqttbridge.exception;

public class MqttPublishInputException extends RuntimeException {

  public MqttPublishInputException(final String message) {
    super(message);
  }

  public MqttPublishInputException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
