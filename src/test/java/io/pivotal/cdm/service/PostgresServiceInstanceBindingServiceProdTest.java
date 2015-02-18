package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.PRODUCTION;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verifyZeroInteractions;
import io.pivotal.cdm.aws.AWSHelper;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.junit.*;
import org.mockito.*;

import com.amazonaws.services.ec2.AmazonEC2Client;

public class PostgresServiceInstanceBindingServiceProdTest {

	@Mock
	AmazonEC2Client ec2Client;

	private PostgresServiceInstanceBindingService bindingService;

	private ServiceInstance serviceInstance = new ServiceInstance(
			"test_service", "test_service_id", PRODUCTION, "1234", "4566", null);

	private ServiceInstanceBinding bindResult;

	private String pgUsername = "pgUser";

	private String pgPassword = "pgPass";

	private String pgURI = "postgres://10.10.10.10/db/test";

	private String serviceId = "postgrescmd";

	private String sourceInstanceId = "source-instance";

	private String subnet = "test_subnet";

	@Mock
	AWSHelper aws;

	private static String bindingId = "test_binding";

	@Before
	public void setUp() throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		bindingService = new PostgresServiceInstanceBindingService(ec2Client,
				pgUsername, pgPassword, pgURI, sourceInstanceId, subnet);
		bindingService.setAWSHelper(aws);
		bindResult = bindingService.createServiceInstanceBinding(bindingId,
				serviceInstance, serviceId, PRODUCTION, "test_app");
		assertThat(bindResult.getId(), is(equalTo(bindingId)));
		assertThat(bindingService.getEC2InstanceForBinding(bindingId),
				is(equalTo(sourceInstanceId)));
		assertThat(bindingService.getAMIForBinding(bindingId),
				is(equalTo(PRODUCTION)));
	}

	@Test
	public void itShouldNotInteractWithAWSForTheProductionCopyDuringBind() {
		verifyZeroInteractions(aws);
	}

	@Test
	public void itShouldNotInteractWithAWSForTheProductionCopyDuringUnbind()
			throws ServiceBrokerException {
		bindingService.deleteServiceInstanceBinding(bindingId, serviceInstance,
				serviceId, PRODUCTION);
		verifyZeroInteractions(aws);
	}

	@Test
	public void itShouldNotReturnInstanceIdsAndAMIAfterUnbind()
			throws ServiceBrokerException {
		bindingService.deleteServiceInstanceBinding(bindingId, serviceInstance,
				serviceId, PRODUCTION);
		assertNull(bindingService.getEC2InstanceForBinding(bindingId));
		assertNull(bindingService.getAMIForBinding(bindingId));
	}

	@Test
	public void itShouldProvideMeProductionCreds() {
		assertThat(bindResult.getCredentials().get("username"),
				is(equalTo(pgUsername)));
		assertThat(bindResult.getCredentials().get("password"),
				is(equalTo(pgPassword)));
		assertThat(bindResult.getCredentials().get("uri"), is(equalTo(pgURI)));
	}
}
