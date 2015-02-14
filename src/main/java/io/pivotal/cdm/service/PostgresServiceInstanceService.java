package io.pivotal.cdm.service;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

/**
 * Creating a service instance is a no op for the copy operations, we simply
 * assume that the prod instance exists.
 * 
 * @author jkruck
 *
 */
@Service
public class PostgresServiceInstanceService implements ServiceInstanceService {

	Map<String, ServiceInstance> instances = new HashMap<String, ServiceInstance>();

	@Override
	public ServiceInstance createServiceInstance(ServiceDefinition service,
			String serviceInstanceId, String planId, String organizationGuid,
			String spaceGuid) throws ServiceInstanceExistsException,
			ServiceBrokerException {
		ServiceInstance instance = new ServiceInstance(serviceInstanceId,
				service.getId(), planId, organizationGuid, spaceGuid, null);
		instances.put(serviceInstanceId, instance);
		return instance;
	}

	@Override
	public ServiceInstance deleteServiceInstance(String id, String serviceId,
			String planId) throws ServiceBrokerException {
		return instances.remove(id);
	}

	@Override
	public ServiceInstance getServiceInstance(String id) {
		return instances.get(id);
	}

	@Override
	public ServiceInstance updateServiceInstance(String instanceId,
			String planId) throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {

		ServiceInstance oldInstance = instances.get(instanceId);
		if (null == oldInstance) {
			throw new ServiceInstanceDoesNotExistException(instanceId);
		}

		instances.put(
				instanceId,
				new ServiceInstance(instanceId, oldInstance
						.getServiceDefinitionId(), planId, oldInstance
						.getOrganizationGuid(), oldInstance.getSpaceGuid(),
						oldInstance.getDashboardUrl()));

		return instances.get(instanceId);
	}
}
