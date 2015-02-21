package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.COPY;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import io.pivotal.cdm.provider.CopyProvider;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.*;
import org.mockito.*;

import com.amazonaws.services.ec2.AmazonEC2Client;

public class PostgresServiceInstanceBindingServiceCopyTest {

	@Mock
	AmazonEC2Client ec2Client;

	private PostgresServiceInstanceBindingService bindingService;

	private ServiceInstance serviceInstance = new ServiceInstance(
			"test_service", "test_service_id", COPY, "1234", "4566", null);

	private ServiceInstanceBinding bindResult;

	private static String bindingId = "test_binding_copy";

	private static String sourceInstanceId = "source_instance";

	@Mock
	CopyProvider provider;

	@Before
	public void setUp() throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		bindingService = new PostgresServiceInstanceBindingService(provider,
				sourceInstanceId);
		when(provider.createCopy(sourceInstanceId)).thenReturn("test_instance");

		bindResult = bindingService.createServiceInstanceBinding(bindingId,
				serviceInstance, "postgrescdm", COPY, "test_app");
	}

	@Test
	public void itShouldBindTheServiceAndSpinUpTheVMs() throws Exception {
		assertThat(bindResult.getId(), is(equalTo(bindingId)));
	}

	@Test
	public void itPlaysItCoolIfItDoesNotHaveAnInstance()
			throws ServiceBrokerException {

		bindingService.deleteServiceInstanceBinding("foo", serviceInstance,
				"postgrescdm", COPY);
	}
}
