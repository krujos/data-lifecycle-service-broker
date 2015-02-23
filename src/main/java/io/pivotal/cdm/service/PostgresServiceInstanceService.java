package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.PostgresCatalogConfig.COPY;
import static io.pivotal.cdm.model.BrokerActionState.*;
import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.model.*;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

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
	private Logger logger = Logger
			.getLogger(PostgresServiceInstanceService.class);

	// The Mapping is Map<serviceInstanceId, Pair<CopyID, ServiceInstance>
	Map<String, Pair<String, ServiceInstance>> instances = new HashMap<String, Pair<String, ServiceInstance>>();

	private CopyProvider provider;

	private String sourceInstanceId;

	private BrokerActionRepository brokerRepo;

	@Autowired
	public PostgresServiceInstanceService(
			final CopyProvider provider,
			@Value("#{environment.SOURCE_INSTANCE_ID}") final String sourceInstanceId,
			BrokerActionRepository brokerRepo) {
		this.provider = provider;
		this.sourceInstanceId = sourceInstanceId;
		this.brokerRepo = brokerRepo;
	}

	@Override
	public ServiceInstance createServiceInstance(ServiceDefinition service,
			String serviceInsatnaceId, String planId, String organizationGuid,
			String spaceGuid) throws ServiceInstanceExistsException,
			ServiceBrokerException {

		log(serviceInsatnaceId, "Creating service instance", IN_PROGRESS);
		throwIfDuplicate(serviceInsatnaceId);

		try {
			ServiceInstance instance = new ServiceInstance(serviceInsatnaceId,
					service.getId(), planId, organizationGuid, spaceGuid, null);
			String copyId = COPY.equals(planId) ? provider
					.createCopy(sourceInstanceId) : sourceInstanceId;
			instances.put(serviceInsatnaceId,
					new MutablePair<String, ServiceInstance>(copyId, instance));

			log(serviceInsatnaceId, "Created service instance", COMPLETE);
			return instance;
		} catch (Exception e) {
			log(serviceInsatnaceId,
					"Failed to create service instance: " + e.getMessage(),
					FAILED);
			throw e;
		}
	}

	@Override
	public ServiceInstance deleteServiceInstance(String id, String serviceId,
			String planId) throws ServiceBrokerException {
		log(id, "Deleting service instance", IN_PROGRESS);
		Pair<String, ServiceInstance> instance = instances.get(id);
		if (null == instance) {
			log(id, "Service instance not found", FAILED);
			return null;
		}

		try {
			if (COPY.equals(planId)) {
				provider.deleteCopy(instance.getLeft());
			}
			log(id, "Deleted service instance", COMPLETE);
			return instances.remove(id).getRight();
		} catch (ServiceBrokerException e) {
			log(id, "Failed to delete service instance: " + e.getMessage(),
					FAILED);
			throw e;
		}
	}

	@Override
	public ServiceInstance getServiceInstance(String id) {
		return instances.containsKey(id) ? instances.get(id).getRight() : null;
	}

	@Override
	public ServiceInstance updateServiceInstance(String id, String planId)
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {

		log(id, "Updating service instance", IN_PROGRESS);
		throwIfNotFound(id);

		ServiceInstance oldInstance = instances.get(id).getRight();
		instances
				.get(id)
				.setValue(
						new ServiceInstance(id, oldInstance
								.getServiceDefinitionId(), planId, oldInstance
								.getOrganizationGuid(), oldInstance
								.getSpaceGuid(), oldInstance.getDashboardUrl()));

		log(id, "Updated service instance", COMPLETE);
		return instances.get(id).getRight();
	}

	private void throwIfNotFound(String id)
			throws ServiceInstanceDoesNotExistException {
		if (!instances.containsKey(id)) {
			log(id, "Cannot find service instance ", FAILED);
			throw new ServiceInstanceDoesNotExistException(id);
		}
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

	private void log(String id, String msg, BrokerActionState state) {
		String logMsg = msg + " " + id;

		if (FAILED == state) {
			logger.error(logMsg);
		} else {
			logger.info(logMsg);
		}
		brokerRepo.save(new BrokerAction(id, state, msg));
	}

	private void throwIfDuplicate(String id)
			throws ServiceInstanceExistsException {
		if (instances.containsKey(id)) {
			log(id, "Duplicate service instance requested", FAILED);
			throw new ServiceInstanceExistsException(instances.get(id)
					.getRight());
		}
	}
}
