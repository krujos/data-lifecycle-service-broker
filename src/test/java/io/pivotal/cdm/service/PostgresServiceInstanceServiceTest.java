package io.pivotal.cdm.service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import io.pivotal.cdm.config.PostgresCatalogConfig;
import io.pivotal.cdm.service.PostgresServiceInstanceService;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.junit.Before;
import org.junit.Test;

public class PostgresServiceInstanceServiceTest {

	private PostgresServiceInstanceService service;
	private ServiceInstance instance;

	@Before
	public void setUp() throws ServiceInstanceExistsException,
			ServiceBrokerException {
		service = new PostgresServiceInstanceService();
		instance = service.createServiceInstance(new PostgresCatalogConfig()
				.catalog().getServiceDefinitions().get(0),
				"service_instance_id", "plan_id", "org_guid", "space_guid");
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
