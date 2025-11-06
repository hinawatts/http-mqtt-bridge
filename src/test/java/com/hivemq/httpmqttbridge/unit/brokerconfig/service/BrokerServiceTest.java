package com.hivemq.httpmqttbridge.unit.brokerconfig.service;

import com.hivemq.httpmqttbridge.brokerconfig.entity.BrokerEntity;
import com.hivemq.httpmqttbridge.brokerconfig.mapper.BrokerConfigurationMapper;
import com.hivemq.httpmqttbridge.brokerconfig.repository.BrokerRepository;
import com.hivemq.httpmqttbridge.brokerconfig.request.BrokerConfigurationRequest;
import com.hivemq.httpmqttbridge.brokerconfig.service.BrokerService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = BrokerServiceTest.class)
public class BrokerServiceTest {

  @Mock private BrokerRepository brokerRepository;

  @InjectMocks private BrokerService brokerService;

  private static final String TEST_HOST = "testHost";

  private static final int TEST_PORT = 1234;

  @Test
  void saveOrUpdate_whenBrokerExists_updatesAndReturnsId() {
    when(brokerRepository.findByHostName(TEST_HOST))
        .thenReturn(java.util.Optional.of(getOldBrokerEntity()));
    when(brokerRepository.save(getBrokerEntity())).thenReturn(getBrokerEntity());
    Long brokerId =
        brokerService.saveOrUpdateBrokerConfiguration(createBrokerConfigurationRequest());
    assert brokerId.equals(1L);
  }

  @Test
  void saveOrUpdate_whenBrokerDoesNotExist_savesAndReturnsId() {

    BrokerConfigurationRequest request = createBrokerConfigurationRequest();

    BrokerEntity mapped = new BrokerEntity();
    mapped.setHostName(TEST_HOST);
    mapped.setPort(TEST_PORT);

    BrokerEntity saved = getBrokerEntity();

    try (var mocked = Mockito.mockStatic(BrokerConfigurationMapper.class)) {
      mocked
          .when(() -> BrokerConfigurationMapper.toBrokerConfigurationEntity(request))
          .thenReturn(mapped);

      when(brokerRepository.save(mapped)).thenReturn(saved);

      Long brokerId = brokerService.saveOrUpdateBrokerConfiguration(request);

      assertEquals(1L, brokerId);
      verify(brokerRepository).findByHostName(TEST_HOST);
      verify(brokerRepository).save(mapped);
      mocked.verify(() -> BrokerConfigurationMapper.toBrokerConfigurationEntity(request));
      verifyNoMoreInteractions(brokerRepository);
    }
    when(brokerRepository.findByHostName(TEST_HOST)).thenReturn(Optional.empty());
    when(brokerRepository.save(getBrokerEntity())).thenReturn(getBrokerEntity());
    Long brokerId =
        brokerService.saveOrUpdateBrokerConfiguration(createBrokerConfigurationRequest());
    assert brokerId.equals(1L);
  }

  private BrokerConfigurationRequest createBrokerConfigurationRequest() {
    return BrokerConfigurationRequest.builder().hostName(TEST_HOST).port(TEST_PORT).build();
  }

  private BrokerEntity getBrokerEntity() {
    return BrokerEntity.builder().id(1L).hostName(TEST_HOST).port(TEST_PORT).build();
  }

  private BrokerEntity getOldBrokerEntity() {
    return BrokerEntity.builder().id(1L).hostName(TEST_HOST).port(TEST_PORT).build();
  }
}
