package io.pivotal.cdm.service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

public class PostgresServiceInstanceBindingServiceTest {

	@Mock
	AmazonEC2Client ec2Client;
	private PostgresServiceInstanceBindingService bindingService;

	private ServiceInstance serviceInstance = new ServiceInstance(
			"test_service", "test_service_id", "copy", "1234", "4566", null);

	private DescribeImagesResult describeImagesResult = new DescribeImagesResult()
			.withImages(new Image().withState("available"));

	private Instance instance = new Instance().withInstanceId("test_instance");

	private RunInstancesResult runInstanceResult = new RunInstancesResult()
			.withReservation(new Reservation().withInstances(Arrays
					.asList(instance)));

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		bindingService = new PostgresServiceInstanceBindingService(ec2Client);
	}

	@Test
	public void itShouldCreateAnImageFromAEC2InstanceAndStartIt()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {

		when(ec2Client.createImage(any())).thenReturn(
				new CreateImageResult().withImageId("test_image"));

		when(ec2Client.describeImages(any())).thenReturn(describeImagesResult);

		when(ec2Client.runInstances(any())).thenReturn(runInstanceResult);

		ServiceInstanceBinding instance = bindingService
				.createServiceInstanceBinding("test_binding", serviceInstance,
						"service_id", "copy", "58839");

		assertThat(instance.getId(), is(equalTo("test_instance")));
	}

	@Test(expected = ServiceBrokerException.class)
	public void itShouldFailWhenImageStateIsFailed()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		when(ec2Client.createImage(any())).thenReturn(
				new CreateImageResult().withImageId("test_image"));

		describeImagesResult.getImages().get(0).setState("failed");
		when(ec2Client.describeImages(any())).thenReturn(describeImagesResult);

		bindingService.createServiceInstanceBinding("test_binding",
				serviceInstance, "service_id", "copy", "58839");
	}
}
