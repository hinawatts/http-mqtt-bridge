package com.hivemq.httpmqttbridge.publish.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublishResponse(Long brokerId, String topic, PublishStatus status,
                              String failureReason) {

}
