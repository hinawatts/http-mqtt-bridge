package com.hivemq.httpmqttbridge.publish.response;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PublishStatus {
  FAILED,
  PUBLISHED;

  @JsonValue
  public String toLowerCase() {
    return name().toLowerCase();
  }
}
