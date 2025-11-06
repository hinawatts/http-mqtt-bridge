package com.hivemq.httpmqttbridge.brokerconfig.response;

import com.hivemq.httpmqttbridge.brokerconfig.domain.Broker;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BrokerResponse {
	private Long brokerId;
	private String hostName;
	private Integer port;

	public static BrokerResponse fromBroker(Broker broker) {
		return BrokerResponse.builder()
				.brokerId(broker.brokerId())
				.hostName(broker.hostName())
				.port(broker.port())
				.build();
	}
}
