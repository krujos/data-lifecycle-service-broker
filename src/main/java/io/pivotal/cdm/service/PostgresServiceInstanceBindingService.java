package io.pivotal.cdm.service;

import static io.pivotal.cdm.model.BrokerActionState.*;
import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.model.*;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import java.util.*;
import java.util.stream.Collectors;

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

	private Logger logger = Logger
			.getLogger(PostgresServiceInstanceBindingService.class);

	private Map<String, ServiceInstanceBinding> instances = new HashMap<String, ServiceInstanceBinding>();

	private PostgresServiceInstanceService instanceService;

	BrokerActionRepository brokerRepo;

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
			PostgresServiceInstanceService instanceService,
			BrokerActionRepository brokerRepo) {
		this.provider = provider;
		this.instanceService = instanceService;
		this.brokerRepo = brokerRepo;
	}

	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(
			CreateServiceInstanceBindingRequest request)
			throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {

		String bindingId = request.getBindingId();
		String appGuid = request.getAppGuid();
		log(bindingId, "Creating service binding for app " + appGuid,
				IN_PROGRESS);

		throwIfDuplicate(bindingId);

		try {
			String instance = instanceService
					.getInstanceIdForServiceInstance(request
							.getServiceInstanceId());

			ServiceInstanceBinding binding = new ServiceInstanceBinding(
					bindingId, request.getServiceInstanceId(),
					provider.getCreds(instance), null, appGuid);

			instances.put(bindingId, binding);
			log(bindingId, "Created service binding for app " + appGuid,
					COMPLETE);
			return binding;
		} catch (Exception e) {
			log(bindingId, "Failed to bind app " + appGuid, FAILED);
			throw e;
		}
	}

	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request)
			throws ServiceBrokerException {
		log(request.getBindingId(), "Removed binding", COMPLETE);
		return instances.remove(request.getBindingId());
	}

	public List<InstancePair> getAppToCopyBinding() {
		//@formatter:off
		return instances
				.values()
				.stream()
				.map(v -> new InstancePair(
						v.getAppGuid(),
						instanceService.getInstanceIdForServiceInstance(
								v.getServiceInstanceId())))
				.collect(Collectors.toList());
		//@formatter:on
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

	private void throwIfDuplicate(String bindingId)
			throws ServiceInstanceBindingExistsException {
		if (instances.containsKey(bindingId)) {
			throw new ServiceInstanceBindingExistsException(
					instances.get(bindingId));
		}
	}
}
