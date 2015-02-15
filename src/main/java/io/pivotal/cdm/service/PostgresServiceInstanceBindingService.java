package io.pivotal.cdm.service;

import io.pivotal.cdm.aws.AWSHelper;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;

@Service
public class PostgresServiceInstanceBindingService implements
		ServiceInstanceBindingService {

	private AWSHelper aws;

	private Logger log = Logger
			.getLogger(PostgresServiceInstanceBindingService.class);

	private Map<String, Object> creds;
	private static String sourceInstanceId = "i-bf72d345";
	private static String desc = "CF Service Broker Snapshot Image";
	private static String subnetId = "subnet-d9b220ae";
	// Left is instance id, right is ami
	private Map<String, ImmutablePair<String, String>> instances = new HashMap<String, ImmutablePair<String, String>>();

	@Autowired
	public PostgresServiceInstanceBindingService(AmazonEC2Client ec2Client) {
		this.aws = new AWSHelper(ec2Client, subnetId);

		creds = new HashMap<String, Object>();
		creds.put("username", "postgres");
		creds.put("password", "postgres");
	}

	/**
	 * Test hook for refactoring. short lived.
	 * 
	 * @param aws
	 */
	protected void setAWSHelper(final AWSHelper aws) {
		this.aws = aws;
	}

	/**
	 * Start up a new EC2 Instance. Create an AMI, then launch the instance.
	 */
	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(
			String bindingId, ServiceInstance serviceInstance,
			String serviceId, String planId, String appGuid)
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {

		String amiId = aws.createAMI(sourceInstanceId, desc);
		String instance = aws.startEC2Instance(amiId);

		instances.put(bindingId, new ImmutablePair<String, String>(instance,
				amiId));
		return new ServiceInstanceBinding(bindingId, serviceInstance.getId(),
				creds, null, appGuid);
	}

	/**
	 * Clean up AWS. Terminate instance, deregister AMI and delete snap.
	 * Terminate deletes the instance volume by default.
	 * 
	 */
	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(
			String bindingId, ServiceInstance instance, String serviceId,
			String planId) throws ServiceBrokerException {

		if (null == instances.get(bindingId)) {
			log.info(bindingId + " not found");
			return null;
		}

		aws.terminateEc2Instance(getEC2InstanceForBinding(bindingId));

		String ami = getAMIForBinding(bindingId);
		aws.deregisterAMI(ami);
		aws.deleteSnapshotsForImage(ami);

		return new ServiceInstanceBinding(bindingId, instance.getId(), null,
				null, null);
	}

	public String getEC2InstanceForBinding(final String bindingId) {
		return (null == instances.get(bindingId)) ? null : instances.get(
				bindingId).getLeft();
	}

	public String getAMIForBinding(String bindingId) {
		return (null == instances.get(bindingId)) ? null : instances.get(
				bindingId).getRight();
	}

}
