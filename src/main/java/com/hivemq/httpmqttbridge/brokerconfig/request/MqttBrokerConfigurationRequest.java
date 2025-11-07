package com.hivemq.httpmqttbridge.brokerconfig.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record MqttBrokerConfigurationRequest(
    @NotBlank(message = "Host Name is required")
    String hostName,

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    Integer port
) {

}