package com.hivemq.httpmqttbridge.brokerconfig.response;

import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBroker;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MqttBrokerResponse {

  private Long brokerId;
  private String hostName;
  private Integer port;

  public static MqttBrokerResponse fromBroker(MqttBroker mqttBroker) {
    return MqttBrokerResponse.builder()
        .brokerId(mqttBroker.brokerId())
        .hostName(mqttBroker.hostName())
        .port(mqttBroker.port())
        .build();
  }
}
