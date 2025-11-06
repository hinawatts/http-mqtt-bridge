package com.hivemq.httpmqttbridge.brokerconfig.repository;

import com.hivemq.httpmqttbridge.brokerconfig.entity.BrokerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface BrokerRepository extends JpaRepository<BrokerEntity, Long> {
    @Modifying
    @Transactional
    @Query(value = """
      INSERT INTO brokers (host_name, port, created_at, updated_at)
      VALUES (:hostName, :port, now(), now())
      ON CONFLICT (host_name)
      DO UPDATE SET
          port = EXCLUDED.port,
          updated_at = now()
      RETURNING id, host_name, port, created_at, updated_at
      """, nativeQuery = true)
    BrokerEntity upsert(@Param("hostName") String hostName,
                        @Param("port") int port);

    Optional<BrokerEntity> findByHostName(String hostName);
}
