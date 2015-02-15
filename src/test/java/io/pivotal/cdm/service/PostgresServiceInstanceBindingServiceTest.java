package io.pivotal.cdm.service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.pivotal.cdm.aws.AWSHelper;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.ec2.AmazonEC2Client;

public class PostgresServiceInstanceBindingServiceTest {

	@Mock
	AmazonEC2Client ec2Client;
	private PostgresServiceInstanceBindingService bindingService;

	private ServiceInstance serviceInstance = new ServiceInstance(
			"test_service", "test_service_id", "copy", "1234", "4566", null);
	private ServiceInstanceBinding bindResult;

	@Before
	public void setUp() throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		MockitoAnnotations.initMocks(this);
		bindingService = new PostgresServiceInstanceBindingService(ec2Client);
		bindingService.setAWSHelper(aws);
		when(aws.createAMI(any(), any())).thenReturn("test_ami");
		when(aws.startEC2Instance("test_ami")).thenReturn("test_ec2instance");
		bindResult = bindingService.createServiceInstanceBinding(
				"test_binding", serviceInstance, "postgrescdm", "copy",
				"test_app");
	}

	@Mock
	AWSHelper aws;

	@Test
	public void itShouldBindTheServiceAndSpinUpTheVMs() throws Exception {

		assertThat(bindResult.getId(), is(equalTo("test_binding")));
		assertThat(bindingService.getAMIForBinding("test_binding"),
				is(equalTo("test_ami")));
		assertThat(bindingService.getEC2InstanceForBinding("test_binding"),
				is(equalTo("test_ec2instance")));

	}

	@Test
	public void itShoudlSaveItsInstanceId()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		assertThat("test_ec2instance",
				is(equalTo(bindingService
						.getEC2InstanceForBinding("test_binding"))));
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
				is(equalTo(bindingService.getAMIForBinding("test_binding"))));
	}

	@Test
	public void itPlaysItCoolIfItDoesNotHaveAnInstance()
			throws ServiceBrokerException {

		bindingService.deleteServiceInstanceBinding("foo", serviceInstance,
				"postgrescdm", "copy");
	}

	@Test
	public void itShouldShutDownTheVMandDeregisterAMIOnUnBind()
			throws Exception {
		bindingService.setAWSHelper(aws);
		ServiceInstanceBinding result = bindingService
				.deleteServiceInstanceBinding("test_binding", serviceInstance,
						"postgrescmd", "copy");

		assertThat(result.getId(), is(equalTo("test_binding")));

		verify(aws).deregisterAMI("test_ami");
		verify(aws).deleteSnapshotsForImage("test_ami");
		verify(aws).terminateEc2Instance("test_ec2instance");
	}
}
