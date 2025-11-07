package com.hivemq.httpmqttbridge.brokerconfig.repository;

import com.hivemq.httpmqttbridge.brokerconfig.entity.MqttBrokerEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MqttBrokerRepository extends JpaRepository<MqttBrokerEntity, Long> {

  Optional<MqttBrokerEntity> findByHostNameAndPort(String hostName, Integer port);
}
