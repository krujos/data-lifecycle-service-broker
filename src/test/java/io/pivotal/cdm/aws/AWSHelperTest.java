package io.pivotal.cdm.aws;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.krujos.test.aws.request.AWSRequestMatcher.awsRqst;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.pivotal.cdm.utils.HostUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;

public class AWSHelperTest {

	@Mock
	private AmazonEC2Client ec2Client;

	private AWSHelper aws;

	private DescribeImagesResult describeImagesResult = new DescribeImagesResult()
			.withImages(new Image().withState("available"));

	private Instance instance = new Instance().withInstanceId("test_instance");

	private RunInstancesResult runInstanceResult = new RunInstancesResult()
			.withReservation(new Reservation().withInstances(Collections
					.singletonList(instance)));

	@Mock
	private HostUtils hostUtils;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		aws = new AWSHelper(ec2Client, "test_subnet", "source_instance",
				hostUtils, 5432);
	}

	@Test
	public void itShouldCreateAnAMI() throws Exception {
		when(
				ec2Client.createImage(awsRqst(r -> r.getInstanceId().equals(
						"test_source_instance")))).thenReturn(
				new CreateImageResult().withImageId("test_image"));

		when(
				ec2Client.describeImages(awsRqst(r -> r.getImageIds().get(0)
						.equals("test_image")))).thenReturn(
				describeImagesResult);

		String amiId = aws
				.createAMI("test_source_instance", "test_description");
		assertThat(amiId, is(equalTo("test_image")));
	}

	@Test
	public void itShouldStartAnEC2InstanceFromAnAMI()
			throws ServiceBrokerException {

		when(
				ec2Client.runInstances(awsRqst(r -> r.getImageId().equals(
						"test_image")))).thenReturn(runInstanceResult);

		when(ec2Client.describeAddresses()).thenReturn(
				new DescribeAddressesResult().withAddresses(Collections
						.singleton(new Address().withPublicIp("10.10.10.10"))));

		when(ec2Client.describeInstanceStatus(any())).thenReturn(
				new DescribeInstanceStatusResult()
						.withInstanceStatuses(Collections
								.singleton(new InstanceStatus()
										.withInstanceState(new InstanceState()
												.withName("running")))));

		when(hostUtils.waitForBoot(anyString(), anyInt())).thenReturn(true);
		assertThat(aws.startEC2Instance("test_image"),
				is(equalTo("test_instance")));
	}

	// TODO this should not throw a service broker exception.
	@Test(expected = TimeoutException.class)
	public void itShouldFailWhenImageStateIsFailed() throws TimeoutException {
		when(ec2Client.createImage(any())).thenReturn(
				new CreateImageResult().withImageId("test_image"));

		describeImagesResult.getImages().get(0).setState("failed");
		when(ec2Client.describeImages(any())).thenReturn(describeImagesResult);

		aws.createAMI("test_source_instance", "it's not gonna work");
	}

	@Test
	public void itShouldTerminateTheInstance() {

		aws.terminateEc2Instance("test_instance");

		verify(ec2Client)
				.terminateInstances(
						awsRqst(r -> r.getInstanceIds().get(0)
								.equals("test_instance")));

	}

	@Test
	public void itShouldDeregisterTheAMI() {
		aws.deregisterAMI("test_image");
		verify(ec2Client).deregisterImage(
				awsRqst(r -> r.getImageId().equals("test_image")));
	}

	@Test
	public void itShouldDeleteTheStorageArtifacts()
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {

		when(ec2Client.describeSnapshots())
				.thenReturn(
						new DescribeSnapshotsResult().withSnapshots(Arrays.asList(
								new Snapshot()
										.withDescription("Created by CreateImage(source_instance) for ami-9687d2fe from vol-e7526fac"),
								new Snapshot()
										.withDescription(
												"Created by CreateImage(source_instance) for test_image from vol-e7526fac")
										.withSnapshotId("test_snapshot"),
								new Snapshot()
										.withDescription("Created by CreateImage(i-bf72d345) for ami-xx from vol-e7526fac"),
								new Snapshot())));

		Predicate<DescribeVolumesRequest> pred = new Predicate<DescribeVolumesRequest>() {
			@Override
			public boolean test(DescribeVolumesRequest r) {
				Filter filter = r.getFilters().stream().findFirst().get();
				return filter.getName().equals("snapshot-id")
						&& filter.getValues().stream().findFirst().get()
								.equals("test_snapshot");
			}
		};

		DescribeVolumesResult volumesResult = new DescribeVolumesResult()
				.withVolumes(Collections.singletonList(new Volume()
						.withVolumeId("test_volume")));

		when(ec2Client.describeVolumes(awsRqst(pred)))
				.thenReturn(volumesResult);

		aws.deleteStorageArtifacts("test_image");

		verify(ec2Client).deleteSnapshot(
				awsRqst(r -> r.getSnapshotId().equals("test_snapshot")));

		verify(ec2Client).deleteVolume(
				awsRqst(r -> r.getVolumeId().equals("test_volume")));

	}

	@Test(expected = ServiceBrokerException.class)
	public void itShouldThrowWhenMoreThanOneAMIIsFoundToBeDeleted()
			throws ServiceBrokerException,
			ServiceInstanceBindingExistsException {

		when(ec2Client.describeSnapshots())
				.thenReturn(
						new DescribeSnapshotsResult().withSnapshots(Arrays.asList(
								new Snapshot()
										.withDescription("Created by CreateImage(source_instance) for test_image from vol-e7526fac"),
								new Snapshot()
										.withDescription(
												"Created by CreateImage(source_instance) for test_image from vol-e7526fac")
										.withSnapshotId("test_snapshot"),
								new Snapshot()
										.withDescription("Created by CreateImage(source_instance) for ami-xx from vol-e7526fac"),
								new Snapshot())));
		aws.deleteStorageArtifacts("test_image");
		verify(ec2Client, never()).deleteSnapshot(any());
	}

	@Test
	public void itShouldGetTheInstanceDNSName() {
		when(ec2Client.describeInstances(any())).thenReturn(
				new DescribeInstancesResult()
						.withReservations(new Reservation()
								.withInstances(new Instance()
										.withPublicIpAddress("0.0.0.0"))));

		assertThat(aws.getEC2InstancePublicIp(instance.getInstanceId()),
				is(equalTo("0.0.0.0")));
	}

	@Test
	public void itShouldReturnAFreeElasticIp() throws ServiceBrokerException {
		when(ec2Client.describeAddresses()).thenReturn(
				new DescribeAddressesResult().withAddresses(Collections
						.singleton(new Address().withPublicIp("10.10.10.10"))));
		assertThat("10.10.10.10", is(aws.getAvaliableElasticIp()));
	}

	@Test
	public void itShouldReturnOnlyOneFreeElasticIp()
			throws ServiceBrokerException {
		when(ec2Client.describeAddresses()).thenReturn(
				new DescribeAddressesResult().withAddresses(Arrays.asList(
						new Address().withPublicIp("10.10.10.10")
								.withInstanceId(null),
						new Address().withPublicIp("10.10.10.11")
								.withInstanceId(null))));
		assertThat("10.10.10.10", is(aws.getAvaliableElasticIp()));
	}

	@Test(expected = ServiceBrokerException.class)
	public void itShouldReturnNullIfNoElasticIpIsAvaliable()
			throws ServiceBrokerException {
		when(ec2Client.describeAddresses()).thenReturn(
				new DescribeAddressesResult().withAddresses(new ArrayList<>()));
		assertNull(aws.getAvaliableElasticIp());
	}

	@Test
	public void itShouldFilterOutAttachedElasticIPs()
			throws ServiceBrokerException {
		when(ec2Client.describeAddresses()).thenReturn(
				new DescribeAddressesResult().withAddresses(Arrays.asList(
						new Address().withPublicIp("10.10.10.10")
								.withInstanceId("the-instance"),
						new Address().withPublicIp("10.10.10.11")
								.withInstanceId(null))));
		assertThat("10.10.10.11", is(aws.getAvaliableElasticIp()));
	}
}
