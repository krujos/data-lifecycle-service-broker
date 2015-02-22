package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.COPY;
import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.provider.CopyProvider;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.*;
import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.*;
import org.cloudfoundry.community.servicebroker.model.*;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.*;
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
	private Logger log = Logger.getLogger(PostgresServiceInstanceService.class);

	// TODO During broker refactor let service instances store user data.
	// The Mapping is Map<SourceInstanceID, Pair<CopyID, ServiceInstance>
	Map<String, Pair<String, ServiceInstance>> instances = new HashMap<String, Pair<String, ServiceInstance>>();

	private CopyProvider provider;

	private String sourceInstanceId;

	@Autowired
	public PostgresServiceInstanceService(
			final CopyProvider provider,
			@Value("#{environment.SOURCE_INSTANCE_ID}") final String sourceInstanceId) {
		this.provider = provider;
		this.sourceInstanceId = sourceInstanceId;
	}

	@Override
	public ServiceInstance createServiceInstance(ServiceDefinition service,
			String serviceInstanceId, String planId, String organizationGuid,
			String spaceGuid) throws ServiceInstanceExistsException,
			ServiceBrokerException {

		log.info("Creating service instance with id " + serviceInstanceId);
		if (instances.containsKey(serviceInstanceId)) {
			throw new ServiceInstanceExistsException(instances.get(
					serviceInstanceId).getRight());
		}

		ServiceInstance instance = new ServiceInstance(serviceInstanceId,
				service.getId(), planId, organizationGuid, spaceGuid, null);
		String id = COPY.equals(planId) ? provider.createCopy(sourceInstanceId)
				: sourceInstanceId;
		instances.put(serviceInstanceId,
				new MutablePair<String, ServiceInstance>(id, instance));
		return instance;
	}

	@Override
	public ServiceInstance deleteServiceInstance(String id, String serviceId,
			String planId) throws ServiceBrokerException {

		log.info("Deleting service instance binding " + id);
		Pair<String, ServiceInstance> instance = instances.get(id);

		if (null == instance) {
			log.info(id + "not found to delete" + id);
			return null;
		}
		if (COPY.equals(planId)) {
			provider.deleteCopy(instance.getLeft());
		}
		return instances.remove(id).getRight();
	}

	@Override
	public ServiceInstance getServiceInstance(String id) {
		return instances.containsKey(id) ? instances.get(id).getRight() : null;
	}

	@Override
	public ServiceInstance updateServiceInstance(String instanceId,
			String planId) throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {

		if (!instances.containsKey(instanceId)) {
			throw new ServiceInstanceDoesNotExistException(instanceId);
		}

		ServiceInstance oldInstance = instances.get(instanceId).getRight();

		instances.get(instanceId).setValue(
				new ServiceInstance(instanceId, oldInstance
						.getServiceDefinitionId(), planId, oldInstance
						.getOrganizationGuid(), oldInstance.getSpaceGuid(),
						oldInstance.getDashboardUrl()));

		return instances.get(instanceId).getRight();
	}

	public String getInstanceIdForServiceInstance(String serviceInstanceId) {
		//@formatter:off
		return instances
				.values()
				.stream()
				.filter(s -> s.getRight().getId().equals(serviceInstanceId))
				.findFirst().
				get().getLeft();
		//@formatter:on
	}

	public List<InstancePair> getProvisionedInstances() {
		//@formatter:off
		return instances.
				values()
				.stream()
				.map(i -> new InstancePair(sourceInstanceId, i.getLeft()))
				.collect(Collectors.toList());
		//@formatter:on
	}

	public String getSourceInstanceId() {
		return sourceInstanceId;
	}
}
