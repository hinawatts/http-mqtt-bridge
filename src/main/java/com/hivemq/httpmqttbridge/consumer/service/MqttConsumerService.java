package com.hivemq.httpmqttbridge.consumer.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface MqttConsumerService {

    void stream(String brokerId, String topic, SseEmitter emitter);
}
