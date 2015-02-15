package io.pivotal.cdm.service;

import java.util.*;

import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
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
	public ServiceInstance createServiceInstance(
			CreateServiceInstanceRequest request)
			throws ServiceInstanceExistsException, ServiceBrokerException {

		ServiceInstance instance = new ServiceInstance(request)
				.withDashboardUrl("http://www.google.com");
		instances.put(request.getServiceInstanceId(), instance);
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

		ServiceInstance instance = instances.get(instanceId);
		if (null == instance) {
			throw new ServiceInstanceDoesNotExistException(instanceId);
		}

		instance.setPlanId(planId);

		instances.put(instanceId, instance);

		return instance;
	}
}
