package com.hivemq.httpmqttbridge.consumer.controller;

import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import com.hivemq.httpmqttbridge.consumer.service.HiveMqttConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/mqtt")
@RequiredArgsConstructor
@Slf4j
public class MqttConsumerController {

    private final HiveMqttConsumerService consumerService;

    @GetMapping(
            path = "/{brokerId}/receive/{topic:.+}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(@PathVariable String brokerId,
                             @PathVariable("topic") String topic){
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        try {
            // Start streaming messages from the MQTT broker
            consumerService.stream(brokerId, topic, emitter);
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Failed to start stream: " + e.getMessage()));
            } catch (Exception ignored) {
                log.error("Failed to send error stream: {}", e.getMessage());
            }
            emitter.completeWithError(e);
        }
        return emitter;
    }


}