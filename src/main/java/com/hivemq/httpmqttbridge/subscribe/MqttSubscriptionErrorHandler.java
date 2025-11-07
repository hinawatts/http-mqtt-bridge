package com.hivemq.httpmqttbridge.subscribe;

import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class MqttSubscriptionErrorHandler {

  public static void sendSseError(SseEmitter emitter, String code, String message, Throwable ex) {
    try {
      emitter.send(SseEmitter.event().name("error").data(Map.of("code", code, "message", message)));
    } catch (Exception ignore) {
    } finally {
      try {
        emitter.complete();
      } catch (Exception ignore) {
      }
    }
  }
}
