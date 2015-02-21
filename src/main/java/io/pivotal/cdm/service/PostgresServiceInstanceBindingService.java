package io.pivotal.cdm.service;

import io.pivotal.cdm.provider.CopyProvider;

import java.util.*;

import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostgresServiceInstanceBindingService implements
		ServiceInstanceBindingService {

	private CopyProvider provider;

	private Logger log = Logger
			.getLogger(PostgresServiceInstanceBindingService.class);

	private Map<String, ServiceInstanceBinding> instances = new HashMap<String, ServiceInstanceBinding>();

	private PostgresServiceInstanceService instanceService;

	/**
	 * Build a new binding service.
	 * 
	 * @param CopyProvider
	 *            to gather credentials from
	 * @param instanceService
	 *            to retrieve instance id's for creds from
	 */
	@Autowired
	public PostgresServiceInstanceBindingService(CopyProvider provider,
			PostgresServiceInstanceService instanceService) {
		this.provider = provider;
		this.instanceService = instanceService;
	}

	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(
			String bindingId, ServiceInstance serviceInstance,
			String serviceId, String planId, String appGuid)
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {

		log.info("Creating service binding for app " + appGuid);

		if (instances.containsKey(bindingId)) {
			throw new ServiceInstanceBindingExistsException(
					instances.get(bindingId));
		}

		String instance = instanceService
				.getInstanceIdForServiceInstance(serviceInstance.getId());

		ServiceInstanceBinding binding = new ServiceInstanceBinding(bindingId,
				serviceInstance.getId(), provider.getCreds(instance), null,
				appGuid);

		instances.put(bindingId, binding);
		return binding;
	}

	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(
			String bindingId, ServiceInstance instance, String serviceId,
			String planId) throws ServiceBrokerException {
		return instances.remove(bindingId);
	}
}
