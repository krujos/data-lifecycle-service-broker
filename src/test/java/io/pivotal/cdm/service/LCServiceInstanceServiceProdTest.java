package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.LCCatalogConfig.COPY;
import static io.pivotal.cdm.config.LCCatalogConfig.PRODUCTION;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import io.pivotal.cdm.config.LCCatalogConfig;
import io.pivotal.cdm.model.BrokerAction;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.SyncTaskExecutor;

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
				brokerRepo, instanceManager, new SyncTaskExecutor());

	}

	private void createServiceInstance() throws Exception {
		ServiceDefinition serviceDef = new LCCatalogConfig().catalog()
				.getServiceDefinitions().get(0);
		CreateServiceInstanceRequest createServiceInstanceRequest = new CreateServiceInstanceRequest(
				serviceDef.getId(), PRODUCTION, "org_guid", "space_guid")
				.withServiceInstanceId("service_instance_id").and()
				.withServiceDefinition(serviceDef).withAsyncClient(true);

		instance = service.createServiceInstance(createServiceInstanceRequest);
		verify(provider, never()).createCopy(any());
	}

	@Test
	public void itShouldNotCreateACopyForProd() throws Exception {
		createServiceInstance();
		verifyZeroInteractions(provider);
	}

	@Test
	public void itShouldNotDeleteACopyForProd() throws Exception {

		createServiceInstance();
		String id = instance.getServiceInstanceId();
		when(instanceManager.getInstance(id)).thenReturn(instance);
		when(instanceManager.removeInstance(id)).thenReturn(instance);

		assertNotNull(service
				.deleteServiceInstance(new DeleteServiceInstanceRequest(id,
						"serviceId", PRODUCTION, true)));
		verifyZeroInteractions(provider);
	}

	@Test
	public void itReturnsTheProdInstanceIdForServiceInstanceId()
			throws Exception {
		createServiceInstance();
		ImmutablePair<String, ServiceInstance> immutablePair = new ImmutablePair<String, ServiceInstance>(
				"source_instance_id", instance);
		when(instanceManager.getInstances()).thenReturn(
				Arrays.asList(immutablePair));
		assertThat(service.getInstanceIdForServiceInstance(instance
				.getServiceInstanceId()), is(equalTo("source_instance_id")));
	}

	@Test
	public void itShouldDocumentItsInFlightCreateActions() throws Exception {
		createServiceInstance();
		verify(brokerRepo, times(2)).save(any(BrokerAction.class));
	}

	@Test
	public void itShouldDocumentItsInFlightDeleteActions() throws Exception {
		createServiceInstance();
		service.deleteServiceInstance(new DeleteServiceInstanceRequest(instance
				.getServiceInstanceId(), "serviceId", PRODUCTION, true));
		verify(brokerRepo, times(4)).save(any(BrokerAction.class));
	}

	@Test(expected = ServiceInstanceUpdateNotSupportedException.class)
	public void itShouldThrowForUpdateService() throws Exception {
		createServiceInstance();
		service.updateServiceInstance(new UpdateServiceInstanceRequest(COPY)
				.withInstanceId(instance.getServiceInstanceId()));
	}
}
