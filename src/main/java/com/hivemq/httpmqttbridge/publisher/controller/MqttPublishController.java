package com.hivemq.httpmqttbridge.publisher.controller;

import com.hivemq.httpmqttbridge.brokerconfig.domain.Broker;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import com.hivemq.httpmqttbridge.exception.BrokerNotFoundException;
import com.hivemq.httpmqttbridge.exception.MqttPublishException;
import com.hivemq.httpmqttbridge.exception.MqttPublishInputException;
import com.hivemq.httpmqttbridge.publisher.service.MqttPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/mqtt")
@RequiredArgsConstructor
@Slf4j
public class MqttPublishController {


    private final MqttPublisher hiveMqttPublisher;

    @PostMapping(
            path = "/{brokerId}/send/{topic:.+}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CompletableFuture<ResponseEntity<Map<String, Object>>> publishMessage(
            @PathVariable Long brokerId,
            @PathVariable String topic,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody Object body
    ) {

        return hiveMqttPublisher.publish(String.valueOf(brokerId), topic, body)
                .thenApply(__ -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "published");
                    result.put("brokerId", brokerId);
                    result.put("topic", topic);
                    return ResponseEntity.accepted().body(result);
                })
                .exceptionally(ex -> {
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;

                    if (cause instanceof MqttPublishInputException e) {
                        throw e;                        // will hit your 400 handler
                    }
                    throw new MqttPublishException(ex.getMessage());
                });
    }


    //TODO: logging, caching and unit tests.
}
