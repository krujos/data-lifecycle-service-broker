package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.LCCatalogConfig.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.config.LCCatalogConfig;
import io.pivotal.cdm.model.BrokerAction;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.*;
import org.mockito.*;

public class LCServiceInstanceServiceProdTest {

	private LCServiceInstanceService service;
	private ServiceInstance instance;

	@Mock
	CopyProvider provider;

	@Mock
	BrokerActionRepository brokerRepo;

	@Mock
	LCServiceInstanceManager instanceManager;

	// TODO DRY w/ copy test
	@Before
	public void setUp() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		service = new LCServiceInstanceService(provider, "source_instance_id",
				brokerRepo, instanceManager);

	}

	private void createServiceInstance() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		ServiceDefinition serviceDef = new LCCatalogConfig().catalog()
				.getServiceDefinitions().get(0);
		CreateServiceInstanceRequest createServiceInstanceRequest = new CreateServiceInstanceRequest(
				serviceDef.getId(), PRODUCTION, "org_guid", "space_guid")
				.withServiceInstanceId("service_instance_id").and()
				.withServiceDefinition(serviceDef);

		instance = service.createServiceInstance(createServiceInstanceRequest);
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
		String id = instance.getServiceInstanceId();
		when(instanceManager.getInstance(id)).thenReturn(instance);
		when(instanceManager.removeInstance(id)).thenReturn(instance);

		assertNotNull(service
				.deleteServiceInstance(new DeleteServiceInstanceRequest(id,
						"serviceId", PRODUCTION)));
		verifyZeroInteractions(provider);
	}

	@Test
	public void itReturnsTheProdInstanceIdForServiceInstanceId()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		ImmutablePair<String, ServiceInstance> immutablePair = new ImmutablePair<String, ServiceInstance>(
				"source_instance_id", instance);
		when(instanceManager.getInstances()).thenReturn(
				Arrays.asList(immutablePair));
		assertThat(service.getInstanceIdForServiceInstance(instance
				.getServiceInstanceId()), is(equalTo("source_instance_id")));
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
		service.deleteServiceInstance(new DeleteServiceInstanceRequest(instance
				.getServiceInstanceId(), "serviceId", PRODUCTION));
		verify(brokerRepo, times(4)).save(any(BrokerAction.class));
	}

	@Test(expected = ServiceInstanceUpdateNotSupportedException.class)
	public void itShouldThrowForUpdateService()
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException,
			ServiceInstanceExistsException {
		createServiceInstance();
		service.updateServiceInstance(new UpdateServiceInstanceRequest(COPY)
				.withInstanceId(instance.getServiceInstanceId()));
	}
}
