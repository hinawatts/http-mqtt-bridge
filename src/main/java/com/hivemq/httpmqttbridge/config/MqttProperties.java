package com.hivemq.httpmqttbridge.config;

import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBrokerCredentials;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

  public static final Long DEFAULT_BROKER_ID = 0L;

  private Map<Long, MqttBrokerCredentials> brokers = new HashMap<>();

}
