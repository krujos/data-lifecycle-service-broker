package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.*;
import io.pivotal.cdm.aws.AWSHelper;

import java.net.*;
import java.util.*;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2Client;

@Service
public class PostgresServiceInstanceBindingService implements
		ServiceInstanceBindingService {

	private AWSHelper aws;

	private Logger log = Logger
			.getLogger(PostgresServiceInstanceBindingService.class);

	private Map<String, Object> creds;

	private static String desc = "CF Service Broker Snapshot Image";

	// Left is instance id, right is ami
	private Map<String, ImmutablePair<String, String>> instances = new HashMap<String, ImmutablePair<String, String>>();

	private String sourceInstanceId;

	/**
	 * Build a new binding service.
	 * 
	 * @param ec2Client
	 *            which is connected to AWS
	 * @param pgUsername
	 *            for the production instance of postgres
	 * @param pgPassword
	 *            for the production instance of postgres
	 * @param pgURI
	 *            pointing to the production instance
	 */
	@Autowired
	public PostgresServiceInstanceBindingService(
			AmazonEC2Client ec2Client,
			@Value("#{environment.PG_USER}") String pgUsername,
			@Value("#{environment.PG_PASSWORD}") String pgPassword,
			@Value("#{environment.PG_URI}") String pgURI,
			@Value("#{environment.SOURCE_INSTANCE_ID}") String sourceInstanceId,
			@Value("#{environment.SUBNET_ID}") String subnetId) {
		this.aws = new AWSHelper(ec2Client, subnetId);

		creds = new HashMap<String, Object>();
		creds.put("username", pgUsername);
		creds.put("password", pgPassword);
		creds.put("uri", pgURI);
		this.sourceInstanceId = sourceInstanceId;

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

		Map<String, Object> instanceCreds = null;
		if (COPY.equals(planId)) {
			log.info("Creating copy instance for app " + appGuid);
			String amiId = aws.createAMI(sourceInstanceId, desc);
			String instance = aws.startEC2Instance(amiId);
			instanceCreds = getCopyCreds(instance);
			instances.put(bindingId, new ImmutablePair<String, String>(
					instance, amiId));
		} else {
			log.info("Creating production instance for app " + appGuid);
			instanceCreds = creds;
			instances.put(bindingId, new ImmutablePair<String, String>(
					sourceInstanceId, PRODUCTION));
		}

		return new ServiceInstanceBinding(bindingId, serviceInstance.getId(),
				instanceCreds, null, appGuid);
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

		if (!sourceInstanceId.equals(getEC2InstanceForBinding(bindingId))) {
			aws.terminateEc2Instance(getEC2InstanceForBinding(bindingId));
			String ami = getAMIForBinding(bindingId);
			aws.deregisterAMI(ami);
			aws.deleteSnapshotsForImage(ami);
		}
		instances.remove(bindingId);
		return new ServiceInstanceBinding(bindingId, instance.getId(), null,
				null, null);
	}

	private Map<String, Object> getCopyCreds(String instance)
			throws ServiceBrokerException {
		String instanceIp = aws.getEC2InstanceIp(instance);
		Map<String, Object> instanceCreds = new HashMap<String, Object>(creds);
		try {
			String pgURI = (String) instanceCreds.get("uri");
			instanceCreds.put("uri",
					pgURI.replace(new URI(pgURI).getHost(), instanceIp));
		} catch (URISyntaxException e) {
			throw new ServiceBrokerException(e);
		}
		return instanceCreds;
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
