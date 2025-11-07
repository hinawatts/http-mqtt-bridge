package com.hivemq.httpmqttbridge.publish.controller;

import com.hivemq.httpmqttbridge.exception.MqttPublishInputException;
import com.hivemq.httpmqttbridge.publish.response.PublishResponse;
import com.hivemq.httpmqttbridge.publish.response.PublishStatus;
import com.hivemq.httpmqttbridge.publish.service.MqttPublisherService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller publish request to Mqtt broker. Provides endpoint to send messages to specified
 * MQTT broker and topic.
 */

@RestController
@RequestMapping("/mqtt")
@RequiredArgsConstructor
@Slf4j
public class MqttPublishController {

  private static final String REQUEST_ID_HEADER = "x-request-id";
  private final MqttPublisherService hiveMqttPublisherService;

  @PostMapping(path = "/{brokerId}/send/{topic:.+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Tag(name = "Publish Messages", description = "API to publish messages to MQTT brokers")
  public CompletableFuture<ResponseEntity<PublishResponse>> publishMessage(
      @PathVariable Long brokerId, @PathVariable String topic,
      @RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId,
      @RequestBody Object body) {
    log.debug("MqttPublishRequest - BrokerId: {}, Topic: {}, RequestId: {}", brokerId, topic,
        requestId);
    validateTopic(topic, requestId);
    final String publishRequestId = requestId == null ? UUID.randomUUID().toString() : requestId;
    return hiveMqttPublisherService.publish(brokerId, topic, body, publishRequestId)
        .orTimeout(8, java.util.concurrent.TimeUnit.SECONDS).thenApply(
            __ -> ResponseEntity.ok().header(REQUEST_ID_HEADER, publishRequestId).body(
                PublishResponse.builder().brokerId(brokerId).topic(topic)
                    .status(PublishStatus.PUBLISHED).build())).exceptionally(ex -> {
          Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;

          if (cause instanceof MqttPublishInputException e) {
            log.error(
                "MqttPublishRequest - Input validation failed for BrokerId: {}, Topic: {}, RequestId: {}, Reason: {}",
                brokerId, topic, publishRequestId, e.getMessage());
            return ResponseEntity.badRequest().header(REQUEST_ID_HEADER, publishRequestId).body(
                PublishResponse.builder().brokerId(brokerId).topic(topic)
                    .status(PublishStatus.FAILED).failureReason(ex.getLocalizedMessage()).build());
          }
          return ResponseEntity.internalServerError().header(REQUEST_ID_HEADER, publishRequestId)
              .body(PublishResponse.builder().brokerId(brokerId).topic(topic)
                  .status(PublishStatus.FAILED).failureReason(ex.getMessage()).build());
        });
  }

  private void validateTopic(String topic, String requestId) {
    if (topic == null || topic.trim().isEmpty()) {
      log.error("MqttPublishRequest - Invalid topic: {} for request - {} ", topic, requestId);
      throw new MqttPublishInputException("Invalid topic: Topic cannot be null or empty");
    }
  }
}
