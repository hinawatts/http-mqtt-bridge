package com.hivemq.httpmqttbridge.brokerconfig.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class BrokerConfigurationRequest {

  @NotBlank(message = "Host Name is required")
  private String hostName;

  @NotNull(message = "Port is required")
  @Min(value = 1, message = "Port must be between 1 and 65535")
  @Max(value = 65535, message = "Port must be between 1 and 65535")
  private Integer port;
}
