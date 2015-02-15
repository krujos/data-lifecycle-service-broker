package io.pivotal.cdm.service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

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

		assertThat(instance.getId(), is(equalTo("test_binding")));
	}

	@Test
	public void itShoudlSaveItsInstanceId()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		this.itShouldCreateAnImageFromAEC2InstanceAndStartIt();
		assertThat("test_instance",
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
		this.itShouldCreateAnImageFromAEC2InstanceAndStartIt();
		assertThat("test_image",
				is(equalTo(bindingService.getAMIForBinding("test_binding"))));
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

	@Test
	public void itShouldTerminateTheInstanceUponUnbind()
			throws ServiceBrokerException,
			ServiceInstanceBindingExistsException {

		this.itShouldCreateAnImageFromAEC2InstanceAndStartIt();

		when(
				ec2Client
						.terminateInstances(argThat(new ArgumentMatcher<TerminateInstancesRequest>() {

							@Override
							public boolean matches(Object argument) {
								return ((TerminateInstancesRequest) argument)
										.getInstanceIds().get(0)
										.equals("test_instance");
							}

						}))).thenReturn(new TerminateInstancesResult());
		bindingService.deleteServiceInstanceBinding("test_binding",
				serviceInstance, "postgrescdm", "copy");
	}

	@Test
	public void itShouldDeregisterTheAMIUponUnbind()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		this.itShouldCreateAnImageFromAEC2InstanceAndStartIt();

		bindingService.deleteServiceInstanceBinding("test_binding",
				serviceInstance, "postgrescdm", "copy");
		verify(ec2Client).deregisterImage(
				argThat(new ArgumentMatcher<DeregisterImageRequest>() {

					@Override
					public boolean matches(Object argument) {
						return ((DeregisterImageRequest) argument).getImageId()
								.equals("test_image");
					}
				}));
	}

	@Test
	public void itShouldDeleteTheSnapShotUponUnbind()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {
		this.itShouldCreateAnImageFromAEC2InstanceAndStartIt();

		when(ec2Client.describeSnapshots())
				.thenReturn(
						new DescribeSnapshotsResult().withSnapshots(Arrays.asList(
								new Snapshot()
										.withDescription("Created by CreateImage(i-bf72d345) for ami-9687d2fe from vol-e7526fac"),
								new Snapshot()
										.withDescription(
												"Created by CreateImage(i-bf72d345) for test_image from vol-e7526fac")
										.withSnapshotId("test_snapshot"),
								new Snapshot()
										.withDescription("Created by CreateImage(i-bf72d345) for ami-xx from vol-e7526fac"),
								new Snapshot())));
		bindingService.deleteServiceInstanceBinding("test_binding",
				serviceInstance, "postgrescdm", "copy");
		verify(ec2Client).deleteSnapshot(
				new DeleteSnapshotRequest().withSnapshotId("test_snapshot"));

	}

	@Test(expected = ServiceBrokerException.class)
	public void itShouldThrowWhenMoreThanOneAMIIsFoundToBeDeleted()
			throws ServiceBrokerException,
			ServiceInstanceBindingExistsException {
		this.itShouldCreateAnImageFromAEC2InstanceAndStartIt();

		when(ec2Client.describeSnapshots())
				.thenReturn(
						new DescribeSnapshotsResult().withSnapshots(Arrays.asList(
								new Snapshot()
										.withDescription("Created by CreateImage(i-bf72d345) for test_image from vol-e7526fac"),
								new Snapshot()
										.withDescription(
												"Created by CreateImage(i-bf72d345) for test_image from vol-e7526fac")
										.withSnapshotId("test_snapshot"),
								new Snapshot()
										.withDescription("Created by CreateImage(i-bf72d345) for ami-xx from vol-e7526fac"),
								new Snapshot())));
		bindingService.deleteServiceInstanceBinding("test_binding",
				serviceInstance, "postgrescdm", "copy");
	}

	@Test
	public void itPlaysItCoolIfItDoesNotHaveAnInstance()
			throws ServiceBrokerException {

		bindingService.deleteServiceInstanceBinding("foo", serviceInstance,
				"postgrescdm", "copy");
	}
}
