package com.hivemq.httpmqttbridge.subscribe.controller;

import com.hivemq.httpmqttbridge.subscribe.service.HiveMqttSubscriptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/mqtt")
@RequiredArgsConstructor
@Slf4j
public class MqttSubscriptionController {

  private final HiveMqttSubscriptionService consumerService;

  @GetMapping(path = "/{brokerId}/receive/{topic:.+}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Tag(name = "Subscribe to Messages", description = "API to subscribe and stream messages from MQTT brokers")
  public SseEmitter stream(@PathVariable Long brokerId, @PathVariable("topic") String topic) {
    SseEmitter emitter = new SseEmitter(0L);
    try {
      // Start streaming messages from the MQTT broker
      consumerService.stream(brokerId, topic, emitter);
    } catch (Exception e) {
      try {
        emitter.send(
            SseEmitter.event().name("error").data("Failed to start stream: " + e.getMessage()));
      } catch (Exception ignored) {
        log.error("Failed to send error stream: {}", e.getMessage());
      }
      emitter.completeWithError(e);
    }
    return emitter;
  }


}