package io.pivotal.cdm.service;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import io.pivotal.cdm.config.PostgresCatalogConfig;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.*;

public class PostgresServiceInstanceServiceTest {

	private PostgresServiceInstanceService service;
	private ServiceInstance instance;

	@Before
	public void setUp() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		service = new PostgresServiceInstanceService();
		String definitionId = new PostgresCatalogConfig().catalog()
				.getServiceDefinitions().get(0).getId();
		CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(
				definitionId, "copy", "test_org", "test_space")
				.withServiceInstanceId("service_instance_id");
		instance = service.createServiceInstance(request);
	}

	@Test
	public void itShouldStoreWhatItCreates()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		assertThat(instance,
				is(equalTo(service.getServiceInstance(instance.getId()))));
	}

	@Test
	public void itShouldUpdateWhatItStores()
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {
		assertThat(service.updateServiceInstance(instance.getId(), "new_plan")
				.getPlanId(), is(equalTo("new_plan")));
		assertThat(service.getServiceInstance(instance.getId()).getPlanId(),
				is(equalTo("new_plan")));
	}

	@Test
	public void itDeletesWhatItShould() throws ServiceBrokerException {
		assertThat(service.deleteServiceInstance(instance.getId(),
				instance.getServiceDefinitionId(), instance.getPlanId()),
				is(equalTo(instance)));
		assertNull(service.getServiceInstance(instance.getId()));
	}

	@Test(expected = ServiceInstanceDoesNotExistException.class)
	public void itShoudlBarfWhenWeUpdateSomethingThatIsNotThere()
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {
		service.updateServiceInstance("fake", "fake");
	}
}
