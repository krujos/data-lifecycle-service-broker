package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.COPY;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.config.PostgresCatalogConfig;
import io.pivotal.cdm.provider.CopyProvider;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.junit.*;
import org.mockito.*;

public class PostgresServiceInstanceServiceCopyTest {

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
}
