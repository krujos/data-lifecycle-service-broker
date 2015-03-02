package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.LCCatalogConfig.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.config.LCCatalogConfig;
import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import java.util.*;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.*;
import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.*;
import org.mockito.*;

public class LCServiceInstanceServiceCopyTest {

	private LCServiceInstanceService service;
	private ServiceInstance instance;

	@Mock
	private CopyProvider provider;

	@Mock
	private BrokerActionRepository brokerRepo;

	private ServiceDefinition serviceDef = new LCCatalogConfig().catalog()
			.getServiceDefinitions().get(0);

	@Mock
	LCServiceInstanceManager instanceManager;

	@Before
	public void setUp() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		service = new LCServiceInstanceService(provider, "source_instance_id",
				brokerRepo, instanceManager);

	}

	private void createServiceInstance() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		when(provider.createCopy("source_instance_id")).thenReturn(
				"copy_instance");
		CreateServiceInstanceRequest createServiceInstanceRequest = new CreateServiceInstanceRequest(
				serviceDef.getId(), COPY, "org_guid", "space_guid")
				.withServiceInstanceId("service_instance_id").and()
				.withServiceDefinition(serviceDef);

		instance = service.createServiceInstance(createServiceInstanceRequest);
	}

	@Test
	public void itShouldStoreWhatItCreates()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		verify(instanceManager).saveInstance(instance, "copy_instance");
	}

	@Test
	public void itShouldCreateACopyWhenProvisionedWithACopyPlan()
			throws ServiceBrokerException, ServiceInstanceExistsException {
		createServiceInstance();
		verify(provider).createCopy("source_instance_id");
	}

	@Test
	public void itDeletesWhatItShould() throws ServiceBrokerException,
			ServiceInstanceExistsException {
		createServiceInstance();
		String id = instance.getServiceInstanceId();
		when(instanceManager.getInstance(id)).thenReturn(instance);
		when(instanceManager.removeInstance(id)).thenReturn(instance);
		when(instanceManager.getCopyIdForInstance(id)).thenReturn(
				"copy_instance");
		assertThat(
				service.deleteServiceInstance(new DeleteServiceInstanceRequest(
						id, instance.getServiceDefinitionId(), instance
								.getPlanId())), is(equalTo(instance)));
		verify(provider).deleteCopy("copy_instance");
	}

	@Test
	public void itReturnsTheCopyInstanceIdForServiceInstanceId()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		Collection<Pair<String, ServiceInstance>> instances = Arrays
				.asList(new ImmutablePair<String, ServiceInstance>(
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
	public void itShouldThrowIfInstanceAlreadyExists()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		when(instanceManager.getInstance(any())).thenReturn(
				new ServiceInstance(new CreateServiceInstanceRequest(null,
						null, null, null)));
		createServiceInstance();
	}

	@Test(expected = ServiceInstanceUpdateNotSupportedException.class)
	public void itShouldThrowForUpdateSErvice()
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException,
			ServiceInstanceExistsException {
		createServiceInstance();
		service.updateServiceInstance(new UpdateServiceInstanceRequest(
				PRODUCTION).withInstanceId(instance.getServiceInstanceId()));

	}
}
