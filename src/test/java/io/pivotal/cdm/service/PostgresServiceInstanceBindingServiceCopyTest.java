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
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

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
			new CreateServiceInstanceRequest("test_service_def_id", COPY,
					"org", "space"));

	private ServiceInstanceBinding bindResult;

	private static String bindingId = "test_binding_copy";

	@Mock
	CopyProvider provider;

	@Mock
	PostgresServiceInstanceService instanceService;

	@Mock
	BrokerActionRepository repo;

	private CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest = new CreateServiceInstanceBindingRequest(
			"postgrescdm", COPY, "test_app").withBindingId(bindingId).and()
			.withServiceInstanceId(serviceInstance.getId());

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

		bindResult = bindingService
				.createServiceInstanceBinding(createServiceInstanceBindingRequest);

		assertThat(bindResult.getId(), is(equalTo(bindingId)));
		assertThat(bindResult.getCredentials().get("uri"),
				is(equalTo("test_uri")));
	}

	@Test
	public void itPlaysItCoolIfItDoesNotHaveAnInstance()
			throws ServiceBrokerException {

		bindingService
				.deleteServiceInstanceBinding(new DeleteServiceInstanceBindingRequest(
						"foo", serviceInstance, "postgrescdm", COPY));
	}

	@Test(expected = ServiceInstanceBindingExistsException.class)
	public void duplicateServiceShouldThrow()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		bindResult = bindingService
				.createServiceInstanceBinding(createServiceInstanceBindingRequest);
		bindResult = bindingService
				.createServiceInstanceBinding(createServiceInstanceBindingRequest);
	}

	@Test
	public void itShouldReturnAppToInstancePairsAndBindToMutipleApps()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		when(
				instanceService.getInstanceIdForServiceInstance(serviceInstance
						.getId())).thenReturn("test_copy");

		IntConsumer consumer = new IntConsumer() {
			@Override
			public void accept(int i) {
				createServiceInstanceBindingRequest.withBindingId("bind" + i)
						.setAppGuid("test_app" + i);
				try {
					bindingService
							.createServiceInstanceBinding(createServiceInstanceBindingRequest);
				} catch (ServiceInstanceBindingExistsException
						| ServiceBrokerException e) {
					fail("Failed to create service instance bindings");
				}
			}
		};
		IntStream.range(1, 4).forEach(consumer);

		List<InstancePair> appBindings = bindingService.getAppToCopyBinding();
		assertThat(appBindings, hasSize(3));
		assertTrue(appBindings.contains(new InstancePair("test_app2",
				"test_copy")));
	}

	@Test
	public void itShouldUpdateItsStatusDuringTheBind()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		bindingService
				.createServiceInstanceBinding(createServiceInstanceBindingRequest);
		verify(repo, times(2)).save(any(BrokerAction.class));
	}
}
