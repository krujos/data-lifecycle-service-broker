package io.pivotal.cdm.aws;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Volume;

public class AWSHelper {
	private Logger log = Logger.getLogger(AWSHelper.class);

	private AmazonEC2Client ec2Client;

	private String subnetId;

	private String sourceInstanceId;

	@Autowired
	public AWSHelper(AmazonEC2Client ec2Client, String subnetId,
			String sourceInstanceId) {
		this.ec2Client = ec2Client;
		this.subnetId = subnetId;
		this.sourceInstanceId = sourceInstanceId;
	}

	public String getEC2InstancePublicIp(String instance) {
		DescribeInstancesResult result = ec2Client
				.describeInstances(new DescribeInstancesRequest()
						.withInstanceIds(instance));
		return result.getReservations().get(0).getInstances().get(0)
				.getPublicIpAddress();
	}

	public void deregisterAMI(String ami) {
		log.info("Deregistering AMI " + ami);
		ec2Client
				.deregisterImage(new DeregisterImageRequest().withImageId(ami));
	}

	public void terminateEc2Instance(String ec2Instance) {
		log.info("Terminating instance " + ec2Instance);
		ec2Client.terminateInstances(new TerminateInstancesRequest()
				.withInstanceIds(Collections.singletonList(ec2Instance)));
	}

	/**
	 * Given an AMI start an EC2 Instance.
	 * 
	 * @param amiId
	 *            to start
	 * @return the id of the running instance.
	 * @throws ServiceBrokerException
	 */
	public String startEC2Instance(String amiId) throws ServiceBrokerException {
		RunInstancesResult instance = ec2Client
				.runInstances(new RunInstancesRequest().withImageId(amiId)
						.withInstanceType("m1.small").withMinCount(1)
						.withMaxCount(1).withSubnetId(subnetId)
						.withInstanceType(InstanceType.T2Micro));

		String instanceId = getInstanceId(instance);
		addElasticIp(instanceId);
		log.info("Instance " + instanceId + " started successfully");
		return instanceId;
	}

	/**
	 * Associate the next available elastic IP with an instance.
	 * 
	 * @param instanceId
	 * @throws ServiceBrokerException
	 */
	public void addElasticIp(String instanceId) throws ServiceBrokerException {
		AssociateAddressRequest addressRequest = new AssociateAddressRequest()
				.withInstanceId(instanceId).withPublicIp(
						getAvaliableElasticIp());
		log.info("Associating " + addressRequest.getPublicIp()
				+ " with instance " + instanceId);
		if (waitForInstance(instanceId)) {
			ec2Client.associateAddress(addressRequest);
		} else {
			throw new ServiceBrokerException(
					"Instance did not transition to 'running' in alotted time.");
		}
	}

	/**
	 * Pull back a list of the elastic ip's that aren't attached to anything and
	 * return the first one.
	 * 
	 * @return the first available IP.
	 * @throws ServiceBrokerException
	 */
	public String getAvaliableElasticIp() throws ServiceBrokerException {
		DescribeAddressesResult result = ec2Client.describeAddresses();
		log.info("Found " + result.getAddresses().size() + " addresses!");
		return result
				.getAddresses()
				.stream()
				.filter(a -> null == a.getInstanceId())
				.findAny()
				.orElseThrow(
						() -> new ServiceBrokerException(
								"No elastic IP's avaliable!")).getPublicIp();

	}

	/**
	 * Build an AMI of a running EC2Instance. Creates a snap which is
	 * disassociated and tracked via a the description.
	 * 
	 * @param sourceInstance
	 *            the EC2 instance to create an AMI from
	 * @param description
	 *            to shove in the console so you know what your looking at
	 * @return id of the ami
	 * @throws TimeoutException
	 *             if the ami isn't available in time.
	 * 
	 * @see #deleteStorageArtifacts(String)
	 */
	public String createAMI(String sourceInstance, String description)
			throws TimeoutException {
		CreateImageResult imageResult = ec2Client
				.createImage(new CreateImageRequest()
						.withInstanceId(sourceInstance)
						.withDescription(description)
						.withName(
								sourceInstance + "-"
										+ System.currentTimeMillis()));

		String amiId = imageResult.getImageId();
		if (!waitForImage(amiId)) {
			throw new TimeoutException(
					"Timed out waiting for amazon to create AMI " + amiId);
		}
		log.info("Created new AMI with ID: " + amiId);
		return amiId;
	}

	/**
	 * Find the snap & volumes associated with the AMI we used and delete it.
	 * AWS doesn't help us out much and the only relationship (as of 2/14/2015)
	 * we can leverage is the description field.
	 * 
	 * @param ami
	 *            to find associated snaps for
	 * @throws ServiceBrokerExceptions
	 */
	public void deleteStorageArtifacts(String ami)
			throws ServiceBrokerException {

		DescribeSnapshotsResult desc = ec2Client.describeSnapshots();
		if (null == desc.getSnapshots()) {
			return;
		}
		List<Snapshot> snapshots = desc.getSnapshots();

		// The only way I can find to track the snaps that get created (but not
		// cleaned up) as part of the ami creation is by the description. This
		// code is brittle and will probably fail in unexpected and glamorous
		// ways.
		String amiDesc = "Created by CreateImage(" + sourceInstanceId
				+ ") for " + ami + " from vol";

		// Would be nice if the aws client return optionals...
		List<Snapshot> matching = snapshots.stream()
				.filter(s -> safeContains(s::getDescription, amiDesc))
				.collect(Collectors.toList());

		switch (matching.size()) {
		case 0:
			// Should this throw? Might have been manually cleaned up...but it
			// may orphan the volume. It's done this way to allow people to
			// create their own instances in AWS and not jack them up by
			// deleting the volume
			log.error("No snapshots found for AMI " + ami);
			break;
		case 1:
			String snap = matching.get(0).getSnapshotId();
			log.info("Deleting snapshot " + snap);
			ec2Client.deleteSnapshot(new DeleteSnapshotRequest()
					.withSnapshotId(snap));

			deleteVolumeForSnap(snap);
			break;
		default:
			throw new ServiceBrokerException(
					"Found too many snapshots for AMI " + ami);
		}
	}

	private void deleteVolumeForSnap(String snap) {

		waitForVolume(snap);
		String volId = getVolume(snap).getVolumeId();
		log.info("Deleting volume " + volId);
		ec2Client.deleteVolume(new DeleteVolumeRequest().withVolumeId(volId));
	}

	private void waitForVolume(String snap) {
		int retries = 0;
		Volume vol = getVolume(snap);
		while ("in-use".equals(vol.getState()) && retries < 5) {
			log.error("Volume is still in use, sleeping 30");
			sleep();
			retries++;
			vol = getVolume(snap);
		}
	}

	private Volume getVolume(String snap) {
		DescribeVolumesResult volumes = ec2Client
				.describeVolumes(new DescribeVolumesRequest()
						.withFilters(new Filter().withName("snapshot-id")
								.withValues(snap)));

		return volumes.getVolumes().stream().findFirst().get();
	}

	private boolean safeContains(Callable<String> s, String c) {
		try {
			return (null != s.call()) && s.call().contains(c);
		} catch (Exception e) {
			log.error(e);
		}
		return false;
	}

	private boolean waitForInstance(String instanceId) {
		log.info("Waiting for instance to transition to running");
		for (int i = 0; i < 5; ++i) {

			DescribeInstanceStatusResult result = ec2Client
					.describeInstanceStatus(new DescribeInstanceStatusRequest()
							.withInstanceIds(instanceId));
			if (!result.getInstanceStatuses().isEmpty()) {
				String state = result.getInstanceStatuses().get(0)
						.getInstanceState().getName();

				log.info("Instance state is " + state);

				if (state.equals("running")) {
					return true;
				}
			}

			sleep();

		}
		return false;
	}

	private boolean waitForImage(String imageId) {
		for (int i = 0; i < 5; i++) {
			String imageState = getImageState(imageId);
			log.info("Image state is " + imageState);
			switch (imageState) {
			case "available":
				return true;
			case "failed":
				return false;
			default:
				log.info("Waiting 30s for AMI " + imageId);
				sleep();
			}
		}
		return false;
	}

	private void sleep() {
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			log.error(e);
		}
	}

	private String getInstanceId(RunInstancesResult instance) {
		return instance.getReservation().getInstances().get(0).getInstanceId();
	}

	private String getImageState(String imageId) {
		String state = "failed";
		DescribeImagesResult result = ec2Client
				.describeImages(new DescribeImagesRequest()
						.withImageIds(imageId));
		if (null != result && null != result.getImages()
				&& !result.getImages().isEmpty()) {
			state = result.getImages().get(0).getState();
		}
		return state;
	}
}
