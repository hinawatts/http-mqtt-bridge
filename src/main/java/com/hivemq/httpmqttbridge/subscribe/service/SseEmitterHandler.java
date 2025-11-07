package com.hivemq.httpmqttbridge.subscribe.service;

import static com.hivemq.httpmqttbridge.subscribe.MqttSubscriptionErrorHandler.sendSseError;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Slf4j
public class SseEmitterHandler {

  void setupEmitterLifecycle(SseEmitter emitter, Long brokerId, String topic,
      Runnable unsubscribeAction) {

    emitter.onCompletion(() -> {
      log.debug("SSE completed for brokerId={} topic={}", brokerId, topic);
      unsubscribeAction.run();
    });

    emitter.onTimeout(() -> {
      log.warn("SSE timeout for brokerId={} topic={}", brokerId, topic);
      sendSseError(emitter, "TIMEOUT", "SSE timed out", null);
      unsubscribeAction.run();
    });

    emitter.onError(ioEx -> {
      log.warn("SSE I/O error for brokerId={} topic={}", brokerId, topic, ioEx);
      sendSseError(emitter, "EMITTER_IO", "SSE I/O error: " + ioEx.getMessage(), ioEx);
      unsubscribeAction.run();
    });
  }

}
