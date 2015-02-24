package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.config.PostgresCatalogConfig;
import io.pivotal.cdm.model.BrokerAction;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.junit.*;
import org.mockito.*;

public class PostgresServiceInstanceServiceProdTest {

	private PostgresServiceInstanceService service;
	private ServiceInstance instance;

	@Mock
	CopyProvider provider;

	@Mock
	BrokerActionRepository brokerRepo;

	@Before
	public void setUp() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		service = new PostgresServiceInstanceService(provider,
				"source_instance_id", brokerRepo);
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

	@Test
	public void itShouldDocumentItsInFlightCreateActions()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		verify(brokerRepo, times(2)).save(any(BrokerAction.class));
	}

	@Test
	public void itShouldDocumentItsInFlightDeleteActions()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		service.deleteServiceInstance(instance.getId(), "serviceId", PRODUCTION);
		verify(brokerRepo, times(4)).save(any(BrokerAction.class));
	}

	@Test(expected = ServiceInstanceUpdateNotSupportedException.class)
	public void itShouldThrowForUpdateService()
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException,
			ServiceInstanceExistsException {
		createServiceInstance();
		service.updateServiceInstance(instance.getId(), COPY);
	}
}
