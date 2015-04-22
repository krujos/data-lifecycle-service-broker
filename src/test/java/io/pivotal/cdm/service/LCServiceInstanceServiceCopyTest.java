package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.LCCatalogConfig.COPY;
import static io.pivotal.cdm.config.LCCatalogConfig.PRODUCTION;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.pivotal.cdm.config.LCCatalogConfig;
import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.provider.DataProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;
import io.pivotal.cdm.utils.HostUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerAsyncRequiredException;
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

public class LCServiceInstanceServiceCopyTest {

	private LCServiceInstanceService service;
	private ServiceInstance instance;

	@Mock
	private CopyProvider copyProvider;

	@Mock
	private DataProvider dataProvider;

	@Mock
	private BrokerActionRepository brokerRepo;

	private ServiceDefinition serviceDef = new LCCatalogConfig().catalog()
			.getServiceDefinitions().get(0);

	@Mock
	LCServiceInstanceManager instanceManager;

	@Mock
	private DataProviderService dataProviderService;

	@Mock
	private HostUtils hostUtils;

	@Before
	public void setUp() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		service = new LCServiceInstanceService(copyProvider, dataProvider,
				"source_instance_id", brokerRepo, instanceManager,
				new SyncTaskExecutor(), dataProviderService, hostUtils);

	}

	private void createServiceInstance() throws ServiceInstanceExistsException,
			ServiceBrokerException, ServiceBrokerAsyncRequiredException {
		when(copyProvider.createCopy("source_instance_id")).thenReturn(
				"copy_instance");
		CreateServiceInstanceRequest createServiceInstanceRequest = newCreateServiceInstanceRequest();

		instance = service.createServiceInstance(createServiceInstanceRequest);
	}

	private CreateServiceInstanceRequest newCreateServiceInstanceRequest() {
		return new CreateServiceInstanceRequest(serviceDef.getId(), COPY,
				"org_guid", "space_guid", true)
				.withServiceInstanceId("service_instance_id").and()
				.withServiceDefinition(serviceDef).withAsyncClient(true);
	}

	@Test
	public void itShouldStoreWhatItCreates()
			throws ServiceInstanceExistsException, ServiceBrokerException,
			ServiceBrokerAsyncRequiredException {
		when(hostUtils.waitForBoot(any())).thenReturn(true);
		createServiceInstance();
		assertThat(instance.getServiceInstanceLastOperation().getState(),
				is(equalTo("succeeded")));
		verify(instanceManager).saveInstance(instance, "copy_instance");
	}

	@Test
	public void itShouldCreateACopyWhenProvisionedWithACopyPlan()
			throws Exception {
		createServiceInstance();
		verify(copyProvider).createCopy("source_instance_id");
	}

	@Test
	public void itShouldSanitizeACopy() throws Exception {
		when(hostUtils.waitForBoot(any())).thenReturn(true);
		createServiceInstance();
		verify(dataProvider).sanitize(anyString(), any());
	}

	@Test
	public void itDeletesWhatItShould() throws Exception {
		createServiceInstance();
		String id = instance.getServiceInstanceId();
		when(instanceManager.getInstance(id)).thenReturn(instance);
		when(instanceManager.removeInstance(id)).thenReturn(instance);
		when(instanceManager.getCopyIdForInstance(id)).thenReturn(
				"copy_instance");
		assertThat(
				service.deleteServiceInstance(new DeleteServiceInstanceRequest(
						id, instance.getServiceDefinitionId(), instance
								.getPlanId(), true)), is(equalTo(instance)));
		verify(copyProvider).deleteCopy("copy_instance");
		verify(instanceManager).removeInstance(instance.getServiceInstanceId());
	}

	@Test
	public void itReturnsTheCopyInstanceIdForServiceInstanceId()
			throws Exception {
		createServiceInstance();
		Collection<Pair<String, ServiceInstance>> instances = Collections
				.singletonList(new ImmutablePair<String, ServiceInstance>(
						"copy_instance", instance));
		when(instanceManager.getInstances()).thenReturn(instances);
		assertThat(service.getInstanceIdForServiceInstance(instance
				.getServiceInstanceId()), is(equalTo("copy_instance")));
	}

	@Test
	public void itReturnsTheCorrectListOfServices()
			throws ServiceBrokerException, ServiceInstanceExistsException {

		Collection<Pair<String, ServiceInstance>> instances = createInstances();

		when(instanceManager.getInstances()).thenReturn(instances);

		List<InstancePair> provisionedInstances = service
				.getProvisionedInstances();
		assertThat(provisionedInstances, hasSize(5));
		assertTrue(provisionedInstances.contains(new InstancePair(
				"source_instance_id", "copy_instance2")));
		assertTrue(provisionedInstances.contains(new InstancePair(
				"source_instance_id", "source_instance_id")));
	}

	private Collection<Pair<String, ServiceInstance>> createInstances()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		Collection<Pair<String, ServiceInstance>> instances = new ArrayList<Pair<String, ServiceInstance>>();
		IntStream.range(0, 4).forEach(
				i -> instances.add(new ImmutablePair<String, ServiceInstance>(
						"copy_instance" + i, null)));
		instances.add(new ImmutablePair<String, ServiceInstance>(
				"source_instance_id", null));
		return instances;
	}

	@Test(expected = ServiceInstanceExistsException.class)
	public void itShouldThrowIfInstanceAlreadyExists() throws Exception {
		when(instanceManager.getInstance(any())).thenReturn(
				new ServiceInstance(new CreateServiceInstanceRequest(null,
						null, null, null, true)));
		createServiceInstance();
	}

	@Test(expected = ServiceInstanceUpdateNotSupportedException.class)
	public void itShouldThrowForUpdateService() throws Exception {
		createServiceInstance();
		service.updateServiceInstance(new UpdateServiceInstanceRequest(
				PRODUCTION, true).withInstanceId(instance
				.getServiceInstanceId()));

	}

	@Test(expected = ServiceBrokerAsyncRequiredException.class)
	public void itShouldThrowForSyncServiceDeletion() throws Exception {
		service.deleteServiceInstance(new DeleteServiceInstanceRequest(null,
				null, null, false));
	}

	@Test(expected = ServiceBrokerAsyncRequiredException.class)
	public void itShouldThrowForSyncServiceCreation() throws Exception {
		service.createServiceInstance(new CreateServiceInstanceRequest()
				.withAsyncClient(false));
	}

	@Test
	public void itShouldSaveTheInstnaceAsFailedIfDeprovisionFails()
			throws Exception {

		ServiceInstance theInstance = new ServiceInstance(
				newCreateServiceInstanceRequest());

		doThrow(new ServiceBrokerException("Problem!")).when(copyProvider)
				.deleteCopy(anyString());

		when(instanceManager.getInstance(anyString())).thenReturn(theInstance);
		when(instanceManager.getCopyIdForInstance(anyString())).thenReturn(
				"copy_id");

		ServiceInstance failedInstance = service
				.deleteServiceInstance(new DeleteServiceInstanceRequest(
						theInstance.getServiceInstanceId(), theInstance
								.getServiceDefinitionId(), COPY, true));

		assertThat(failedInstance.getServiceInstanceLastOperation().getState(),
				is(equalTo("failed")));

		// Once for in progress, once for failed.
		verify(instanceManager, times(2)).saveInstance(any(), anyString());
		assertTrue(failedInstance.isAsync());
	}
}
