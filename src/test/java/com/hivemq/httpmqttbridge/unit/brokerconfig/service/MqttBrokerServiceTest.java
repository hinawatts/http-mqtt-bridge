package com.hivemq.httpmqttbridge.unit.brokerconfig.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hivemq.httpmqttbridge.brokerconfig.domain.MqttBroker;
import com.hivemq.httpmqttbridge.brokerconfig.entity.MqttBrokerEntity;
import com.hivemq.httpmqttbridge.brokerconfig.mapper.MqttBrokerConfigurationMapper;
import com.hivemq.httpmqttbridge.brokerconfig.repository.MqttBrokerRepository;
import com.hivemq.httpmqttbridge.brokerconfig.request.MqttBrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.brokerconfig.service.MqttBrokerService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class MqttBrokerServiceTest {

  @Mock
  private MqttBrokerRepository mqttBrokerRepository;

  @InjectMocks
  private MqttBrokerService mqttBrokerService;

  private static final String TEST_HOST = "testHost";
  private static final int TEST_PORT = 1234;

  @Test
  void saveOrUpdate_whenBrokerExists_returnsMappedDomain() {

    MqttBrokerEntity existing = MqttBrokerEntity.builder()
        .id(1L).hostName(TEST_HOST).port(TEST_PORT).build();
    when(mqttBrokerRepository.findByHostNameAndPort(TEST_HOST, TEST_PORT))
        .thenReturn(Optional.of(existing));

    try (var mocked = Mockito.mockStatic(MqttBrokerConfigurationMapper.class)) {
      MqttBroker expected = MqttBroker.builder()
          .brokerId(1L).hostName(TEST_HOST).port(TEST_PORT).build();
      mocked.when(() -> MqttBrokerConfigurationMapper.toMqttBroker(existing))
          .thenReturn(expected);

      MqttBroker result = mqttBrokerService.saveOrUpdateBrokerConfiguration(
          MqttBrokerConfigurationRequest.builder().hostName(TEST_HOST).port(TEST_PORT).build()
      );

      assertEquals(1L, result.brokerId());
      assertEquals(TEST_HOST, result.hostName());
      assertEquals(TEST_PORT, result.port());

      verify(mqttBrokerRepository).findByHostNameAndPort(TEST_HOST, TEST_PORT);
      verifyNoMoreInteractions(mqttBrokerRepository);
      mocked.verify(() -> MqttBrokerConfigurationMapper.toMqttBroker(existing));
    }
  }

  @Test
  void saveOrUpdate_whenBrokerDoesNotExist_savesAndReturnsMappedDomain() {

    MqttBrokerConfigurationRequest request =
        MqttBrokerConfigurationRequest.builder().hostName(TEST_HOST).port(TEST_PORT).build();

    MqttBrokerEntity toSave = MqttBrokerEntity.builder()
        .hostName(TEST_HOST).port(TEST_PORT).build();
    MqttBrokerEntity saved = MqttBrokerEntity.builder()
        .id(1L).hostName(TEST_HOST).port(TEST_PORT).build();
    MqttBroker mapped = MqttBroker.builder()
        .brokerId(1L).hostName(TEST_HOST).port(TEST_PORT).build();

    when(mqttBrokerRepository.findByHostNameAndPort(TEST_HOST, TEST_PORT))
        .thenReturn(Optional.empty());
    when(mqttBrokerRepository.save(toSave)).thenReturn(saved);

    try (var mocked = Mockito.mockStatic(MqttBrokerConfigurationMapper.class)) {
      mocked.when(() -> MqttBrokerConfigurationMapper.toBrokerConfigurationEntity(request))
          .thenReturn(toSave);
      mocked.when(() -> MqttBrokerConfigurationMapper.toMqttBroker(saved))
          .thenReturn(mapped);

      MqttBroker result = mqttBrokerService.saveOrUpdateBrokerConfiguration(request);

      assertEquals(1L, result.brokerId());
      assertEquals(TEST_HOST, result.hostName());
      assertEquals(TEST_PORT, result.port());

      verify(mqttBrokerRepository).findByHostNameAndPort(TEST_HOST, TEST_PORT);
      verify(mqttBrokerRepository).save(toSave);
      verifyNoMoreInteractions(mqttBrokerRepository);

      mocked.verify(() -> MqttBrokerConfigurationMapper.toBrokerConfigurationEntity(request));
      mocked.verify(() -> MqttBrokerConfigurationMapper.toMqttBroker(saved));
    }
  }
}
