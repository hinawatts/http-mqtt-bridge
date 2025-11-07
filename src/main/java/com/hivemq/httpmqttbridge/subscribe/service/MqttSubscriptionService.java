package com.hivemq.httpmqttbridge.subscribe.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface MqttSubscriptionService {

  void stream(Long brokerId, String topic, SseEmitter emitter);

}
