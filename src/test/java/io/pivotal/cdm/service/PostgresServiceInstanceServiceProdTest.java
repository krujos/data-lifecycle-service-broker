package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.PRODUCTION;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.config.PostgresCatalogConfig;
import io.pivotal.cdm.provider.CopyProvider;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.junit.*;
import org.mockito.*;

public class PostgresServiceInstanceServiceProdTest {

	private PostgresServiceInstanceService service;
	private ServiceInstance instance;

	@Mock
	CopyProvider provider;

	@Before
	public void setUp() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		service = new PostgresServiceInstanceService(provider,
				"source_instance_id");
	}

	private void createServiceInstance() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		instance = service.createServiceInstance(new PostgresCatalogConfig()
				.catalog().getServiceDefinitions().get(0),
				"service_instance_id", PRODUCTION, "org_guid", "space_guid");
		verify(provider, never()).createCopy(any());
	}

	@Test
	public void itShouldNotCreateACopyForProd()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		verifyZeroInteractions(provider);
	}

	@Test
	public void itShouldNotDeleteACopyForProd() throws ServiceBrokerException,
			ServiceInstanceExistsException {
		createServiceInstance();
		assertNotNull(service.deleteServiceInstance(instance.getId(),
				"serviceId", PRODUCTION));
		verifyZeroInteractions(provider);
	}

	@Test
	public void itReturnsTheProdInstanceIdForServiceInstanceId()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		assertThat(service.getInstanceIdForServiceInstance(instance.getId()),
				is(equalTo("source_instance_id")));
	}
}
