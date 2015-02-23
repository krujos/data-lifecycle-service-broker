package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.COPY;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.model.BrokerAction;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import java.util.*;

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

	@Mock
	CopyProvider provider;

	@Mock
	PostgresServiceInstanceService instanceService;

	@Mock
	BrokerActionRepository repo;

	@Before
	public void setUp() throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		bindingService = new PostgresServiceInstanceBindingService(provider,
				instanceService, repo);
	}

	@Test
	public void itShouldProvideTheCopyCreds() throws Exception {

		Map<String, Object> testCreds = new HashMap<String, Object>();
		testCreds.put("uri", "test_uri");

		when(provider.getCreds("test_instance")).thenReturn(testCreds);

		when(
				instanceService.getInstanceIdForServiceInstance(serviceInstance
						.getId())).thenReturn("test_instance");

		bindResult = bindingService.createServiceInstanceBinding(bindingId,
				serviceInstance, "postgrescdm", COPY, "test_app");

		assertThat(bindResult.getId(), is(equalTo(bindingId)));
		assertThat(bindResult.getCredentials().get("uri"),
				is(equalTo("test_uri")));
	}

	@Test
	public void itPlaysItCoolIfItDoesNotHaveAnInstance()
			throws ServiceBrokerException {

		bindingService.deleteServiceInstanceBinding("foo", serviceInstance,
				"postgrescdm", COPY);
	}

	@Test(expected = ServiceInstanceBindingExistsException.class)
	public void duplicateServiceShouldThrow()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		bindResult = bindingService.createServiceInstanceBinding(bindingId,
				serviceInstance, "postgrescdm", COPY, "test_app");
		bindResult = bindingService.createServiceInstanceBinding(bindingId,
				serviceInstance, "postgrescdm", COPY, "test_app");
	}

	@Test
	public void itShouldReturnAppToInstancePairsAndBindToMutipleApps()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		when(
				instanceService.getInstanceIdForServiceInstance(serviceInstance
						.getId())).thenReturn("test_copy");
		bindingService.createServiceInstanceBinding("bind1", serviceInstance,
				"postgrescdm", COPY, "test_app");
		bindingService.createServiceInstanceBinding("bind2", serviceInstance,
				"postgrescdm", COPY, "test_app2");
		bindingService.createServiceInstanceBinding("bind3", serviceInstance,
				"postgrescdm", COPY, "test_app3");

		List<InstancePair> appBindings = bindingService.getAppToCopyBinding();
		assertThat(appBindings, hasSize(3));
		assertTrue(appBindings.contains(new InstancePair("test_app2",
				"test_copy")));
	}

	@Test
	public void itShouldUpdateItsStatusDuringTheBind()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		bindingService.createServiceInstanceBinding("bind1", serviceInstance,
				"postgrescdm", COPY, "test_app");
		verify(repo, times(2)).save(any(BrokerAction.class));
	}
}
