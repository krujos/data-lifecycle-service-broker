package io.pivotal.cdm.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

@Service
public class PostgresServiceInstanceBindingService implements
		ServiceInstanceBindingService {

	private Logger log = Logger
			.getLogger(PostgresServiceInstanceBindingService.class);
	private AmazonEC2Client ec2Client;
	private Map<String, Object> creds;
	private static String instanceId = "i-bf72d345";
	private static String desc = "CF Service Broker Snapshot Image";
	private static String subnetId = "subnet-d9b220ae";

	@Autowired
	public PostgresServiceInstanceBindingService(AmazonEC2Client ec2Client) {
		this.ec2Client = ec2Client;
		creds = new HashMap<String, Object>();
		creds.put("username", "postgres");
		creds.put("password", "postgres");
	}

	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(
			String bindingId, ServiceInstance serviceInstance,
			String serviceId, String planId, String appGuid)
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {

		CreateImageResult imageResult = ec2Client
				.createImage(new CreateImageRequest()
						.withInstanceId(instanceId)
						.withDescription(desc)
						.withName(instanceId + "-" + System.currentTimeMillis()));

		log.info("Created new AMI with ID: " + imageResult.getImageId());

		if (!waitForImage(imageResult)) {
			throw new ServiceBrokerException(
					"Timed out waiting for amazon to create AMI");
		}

		RunInstancesResult instance = ec2Client
				.runInstances(new RunInstancesRequest()
						.withImageId(imageResult.getImageId())
						.withInstanceType("m1.small").withMinCount(1)
						.withMaxCount(1).withSubnetId(subnetId)
						.withInstanceType(InstanceType.T2Micro));

		log.info("Started instance:" + getInstanceId(instance));

		return new ServiceInstanceBinding(getInstanceId(instance),
				serviceInstance.getId(), creds, null, appGuid);
	}

	private String getInstanceId(RunInstancesResult instance) {
		return instance.getReservation().getInstances().get(0).getInstanceId();
	}

	private boolean waitForImage(CreateImageResult imageResult) {

		return IntStream.range(0, 5).anyMatch(new IntPredicate() {
			@Override
			public boolean test(int i) {
				String imageState = getImageState(imageResult.getImageId());
				log.info("Image state is " + imageState);
				if ("available".equals(imageState)) {
					return true;
				}
				if ("failed".equals(imageState)) {
					log.error("Failed creating AMI, aws does this sometimes... why");
					return false;
				}
				try {
					log.info("Waiting 30 more seconds for AMI creation ("
							+ imageResult.getImageId() + ")");
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return false;
			}
		});
	}

	private String getImageState(String imageId) {
		return ec2Client
				.describeImages(
						new DescribeImagesRequest().withImageIds(imageId))
				.getImages().get(0).getState();
	}

	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(
			String bindingId, ServiceInstance instance, String serviceId,
			String planId) throws ServiceBrokerException {
		// TODO Auto-generated method stub
		return null;
	}

}
