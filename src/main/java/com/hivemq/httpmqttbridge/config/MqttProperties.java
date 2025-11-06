package com.hivemq.httpmqttbridge.config;

import com.hivemq.httpmqttbridge.brokerconfig.domain.BrokerCredentials;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    public static final String DEFAULT_BROKER_ID = "default";

    private Map<String, BrokerCredentials> brokers = new HashMap<>();

    public void setBrokers(Map<String, BrokerCredentials> brokers) {
        this.brokers = brokers;
    }
}
