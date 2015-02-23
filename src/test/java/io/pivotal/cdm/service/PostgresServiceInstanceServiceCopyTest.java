package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.config.PostgresCatalogConfig;
import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.junit.*;
import org.mockito.*;

public class PostgresServiceInstanceServiceCopyTest {

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
		when(provider.createCopy("source_instance_id")).thenReturn(
				"copy_instance");
		instance = service.createServiceInstance(new PostgresCatalogConfig()
				.catalog().getServiceDefinitions().get(0),
				"service_instance_id", COPY, "org_guid", "space_guid");
	}

	@Test
	public void itShouldStoreWhatItCreates()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		assertThat(instance,
				is(equalTo(service.getServiceInstance(instance.getId()))));
	}

	@Test
	public void itShouldUpdateWhatItStores()
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException,
			ServiceInstanceExistsException {
		createServiceInstance();
		assertThat(service.updateServiceInstance(instance.getId(), "new_plan")
				.getPlanId(), is(equalTo("new_plan")));
		assertThat(service.getServiceInstance(instance.getId()).getPlanId(),
				is(equalTo("new_plan")));
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
		assertThat(service.deleteServiceInstance(instance.getId(),
				instance.getServiceDefinitionId(), instance.getPlanId()),
				is(equalTo(instance)));
		assertNull(service.getServiceInstance(instance.getId()));
		verify(provider).deleteCopy("copy_instance");
	}

	@Test(expected = ServiceInstanceDoesNotExistException.class)
	public void itShoudlBarfWhenWeUpdateSomethingThatIsNotThere()
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {
		service.updateServiceInstance("fake", "fake");
	}

	@Test
	public void itReturnsTheCopyInstanceIdForServiceInstanceId()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		assertThat(service.getInstanceIdForServiceInstance(instance.getId()),
				is(equalTo("copy_instance")));
	}

	@Test
	public void itReturnsTheCorrectListOfServices()
			throws ServiceBrokerException, ServiceInstanceExistsException {

		when(provider.createCopy("source_instance_id")).thenReturn(
				"copy_instance1", "copy_instance2", "copy_instance3",
				"copy_instance4");

		IntConsumer creator = new IntConsumer() {

			@Override
			public void accept(int i) {
				try {
					service.createServiceInstance(new PostgresCatalogConfig()
							.catalog().getServiceDefinitions().get(0),
							"service_instance_id_" + i, COPY, "org_guid",
							"space_guid");
				} catch (ServiceInstanceExistsException
						| ServiceBrokerException e) {
					fail(e.getMessage());
				}
			}
		};
		IntStream.range(0, 4).forEach(creator);
		service.createServiceInstance(new PostgresCatalogConfig().catalog()
				.getServiceDefinitions().get(0), "service_instance_id_" + 5,
				PRODUCTION, "org_guid", "space_guid");

		List<InstancePair> instances = service.getProvisionedInstances();
		assertThat(instances, hasSize(5));
		assertTrue(instances.contains(new InstancePair("source_instance_id",
				"copy_instance2")));
		assertTrue(instances.contains(new InstancePair("source_instance_id",
				"source_instance_id")));
	}

	@Test(expected = ServiceInstanceExistsException.class)
	public void itShouldThrowIfInstanceAlreadyExists()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		createServiceInstance();
		createServiceInstance();
	}
}
