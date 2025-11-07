package com.hivemq;

import com.hivemq.httpmqttbridge.config.MqttProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MqttProperties.class)
public class HttpMqttBridgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(HttpMqttBridgeApplication.class, args);
    }
}
