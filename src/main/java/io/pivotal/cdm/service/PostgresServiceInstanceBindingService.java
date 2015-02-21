package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.*;
import io.pivotal.cdm.provider.CopyProvider;

import java.util.*;

import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

@Service
public class PostgresServiceInstanceBindingService implements
		ServiceInstanceBindingService {

	private CopyProvider provider;

	private Logger log = Logger
			.getLogger(PostgresServiceInstanceBindingService.class);

	private String sourceInstance;

	private Map<String, String> instances = new HashMap<String, String>();

	/**
	 * Build a new binding service.
	 * 
	 * @param CopyProvider
	 *            to interact with which is connected to AWS
	 * @param sourceInstanceId
	 *            of the thing running in production (presumably to copy from)
	 */
	@Autowired
	public PostgresServiceInstanceBindingService(CopyProvider provider,
			@Value("#{environment.SOURCE_INSTANCE_ID}") String sourceInstance) {
		this.provider = provider;
		this.sourceInstance = sourceInstance;
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
		log.info("Creating service binding for app " + appGuid);

		String instance = PRODUCTION;
		if (COPY.equals(planId)) {
			log.info("Creating copy instance for app " + appGuid);
			instance = provider.createCopy(sourceInstance);
		}
		instances.put(bindingId, instance);
		return new ServiceInstanceBinding(bindingId, serviceInstance.getId(),
				provider.getCreds(sourceInstance), null, appGuid);
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

		String boundInstance = instances.get(bindingId);
		if (null == boundInstance) {
			log.info(bindingId + " not found, nothing to unbind");
			return null;
		}

		if (!boundInstance.equals(PRODUCTION)) {
			provider.deleteCopy(boundInstance);
		}
		instances.remove(bindingId);
		return new ServiceInstanceBinding(bindingId, instance.getId(), null,
				null, null);
	}
}
