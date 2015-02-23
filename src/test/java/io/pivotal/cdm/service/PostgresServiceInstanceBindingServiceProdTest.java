package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.PRODUCTION;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import java.util.*;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.*;
import org.mockito.*;

import com.amazonaws.services.ec2.AmazonEC2Client;

public class PostgresServiceInstanceBindingServiceProdTest {

	@Mock
	AmazonEC2Client ec2Client;

	private PostgresServiceInstanceBindingService bindingService;

	private ServiceInstance serviceInstance = new ServiceInstance(
			"test_service", "test_service_id", PRODUCTION, "1234", "4566", null);

	private String serviceId = "postgrescmd";

	@Mock
	CopyProvider provider;

	private static String bindingId = "test_binding";

	@Mock
	PostgresServiceInstanceService instanceService;

	@Mock
	BrokerActionRepository repo;

	@Before
	public void setUp() throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		bindingService = new PostgresServiceInstanceBindingService(provider,
				instanceService, repo);

	}

	@Test
	public void itShouldReturnTheProdCreds() throws ServiceBrokerException,
			ServiceInstanceBindingExistsException {

		Map<String, Object> testCreds = new HashMap<String, Object>();
		testCreds.put("uri", "prod_uri");
		when(provider.getCreds("source_instance")).thenReturn(testCreds);

		ServiceInstanceBinding bindResult = bindingService
				.createServiceInstanceBinding(bindingId, serviceInstance,
						serviceId, PRODUCTION, "test_app");
		assertThat(bindResult.getId(), is(equalTo(bindingId)));
	}

	@Test
	public void itShouldNotInteractWithProviderForTheProductionCopyDuringUnbind()
			throws ServiceBrokerException {
		bindingService.deleteServiceInstanceBinding(bindingId, serviceInstance,
				serviceId, PRODUCTION);
		verify(provider, never()).deleteCopy(any());
	}
}
