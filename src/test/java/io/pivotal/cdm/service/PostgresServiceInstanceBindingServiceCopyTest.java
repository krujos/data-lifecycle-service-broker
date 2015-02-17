package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.COPY;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import io.pivotal.cdm.aws.AWSHelper;

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

	@Mock
	AWSHelper aws;

	private String pgUsername = "pgUser";

	private String pgPassword = "pgPass";

	private String pgURI = "postgres://10.10.10.10/db/test";

	private String bindingId = "test_binding_copy";

	private String serviceId = "service_id";

	@Before
	public void setUp() throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		bindingService = new PostgresServiceInstanceBindingService(ec2Client,
				pgUsername, pgPassword, pgURI);
		bindingService.setAWSHelper(aws);
		when(aws.createAMI(any(), any())).thenReturn("test_ami");
		when(aws.startEC2Instance("test_ami")).thenReturn("test_ec2instance");
		when(aws.getEC2InstanceIp(any())).thenReturn("1.1.1.1");

		bindResult = bindingService.createServiceInstanceBinding(bindingId,
				serviceInstance, "postgrescdm", COPY, "test_app");
	}

	@Test
	public void itShouldBindTheServiceAndSpinUpTheVMs() throws Exception {

		assertThat(bindResult.getId(), is(equalTo(bindingId)));
		assertThat(bindingService.getAMIForBinding(bindingId),
				is(equalTo("test_ami")));
		assertThat(bindingService.getEC2InstanceForBinding(bindingId),
				is(equalTo("test_ec2instance")));

	}

	@Test
	public void itShoudlSaveItsInstanceId()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		assertThat("test_ec2instance",
				is(equalTo(bindingService.getEC2InstanceForBinding(bindingId))));
	}

	@Test
	public void itShouldReturnNullAMIForAnUnknownBindingID() {
		assertNull(bindingService.getAMIForBinding("unknown"));
	}

	@Test
	public void itShouldReturnNullInstanceIdForUnknownBindingID() {
		assertNull(bindingService.getEC2InstanceForBinding("unknown"));
	}

	@Test
	public void itShoudlSaveItsAMIId()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		assertThat("test_ami",
				is(equalTo(bindingService.getAMIForBinding(bindingId))));
	}

	@Test
	public void itPlaysItCoolIfItDoesNotHaveAnInstance()
			throws ServiceBrokerException {

		bindingService.deleteServiceInstanceBinding("foo", serviceInstance,
				"postgrescdm", COPY);
	}

	@Test
	public void itShouldShutDownTheVMandDeregisterAMIOnUnBind()
			throws Exception {
		ServiceInstanceBinding result = bindingService
				.deleteServiceInstanceBinding(bindingId, serviceInstance,
						serviceId, COPY);

		assertThat(result.getId(), is(equalTo(bindingId)));

		verify(aws).deregisterAMI("test_ami");
		verify(aws).deleteSnapshotsForImage("test_ami");
		verify(aws).terminateEc2Instance("test_ec2instance");
	}

	@Test
	public void itShouldNotReturnInstanceIdsAndAMIAfterUnbind()
			throws ServiceBrokerException {
		bindingService.deleteServiceInstanceBinding(bindingId, serviceInstance,
				serviceId, COPY);
		assertNull(bindingService.getEC2InstanceForBinding(bindingId));
		assertNull(bindingService.getAMIForBinding(bindingId));
	}

	@Test
	public void itShouldNotPointAtTheProdIP() {
		assertThat(bindResult.getCredentials().get("uri"), is(not(pgURI)));
	}
}
