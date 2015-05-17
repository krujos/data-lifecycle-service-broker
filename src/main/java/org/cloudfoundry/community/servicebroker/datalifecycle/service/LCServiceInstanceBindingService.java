package org.cloudfoundry.community.servicebroker.datalifecycle.service;

import static org.cloudfoundry.community.servicebroker.datalifecycle.model.BrokerActionState.COMPLETE;
import static org.cloudfoundry.community.servicebroker.datalifecycle.model.BrokerActionState.FAILED;
import static org.cloudfoundry.community.servicebroker.datalifecycle.model.BrokerActionState.IN_PROGRESS;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.datalifecycle.dto.InstancePair;
import org.cloudfoundry.community.servicebroker.datalifecycle.model.BrokerAction;
import org.cloudfoundry.community.servicebroker.datalifecycle.model.BrokerActionState;
import org.cloudfoundry.community.servicebroker.datalifecycle.provider.CopyProvider;
import org.cloudfoundry.community.servicebroker.datalifecycle.repo.BrokerActionRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LCServiceInstanceBindingService implements
		ServiceInstanceBindingService {

	private CopyProvider provider;

	private Logger logger = Logger
			.getLogger(LCServiceInstanceBindingService.class);

	private LCServiceInstanceBindingManager bindings;

	private LCServiceInstanceService instanceService;

	private BrokerActionRepository brokerRepo;

	/**
	 * Build a new binding service.
	 * 
	 * @param CopyProvider
	 *            to gather credentials from
	 * @param instanceService
	 *            to get instance information from
	 * @param brokerRepo
	 *            to save current action states to
	 * @param bindings
	 *            manager to save bindings
	 * @param instanceService
	 *            to retrieve instance id's for creds from
	 */
	@Autowired
	public LCServiceInstanceBindingService(CopyProvider provider,
			LCServiceInstanceService instanceService,
			BrokerActionRepository brokerRepo,
			LCServiceInstanceBindingManager bindings) {
		this.provider = provider;
		this.instanceService = instanceService;
		this.brokerRepo = brokerRepo;
		this.bindings = bindings;
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

		throwIfDuplicateBinding(bindingId);
		throwIfCopyAlreadyBoundToApp(appGuid, request.getServiceInstanceId());

		try {
			String instance = instanceService
					.getInstanceIdForServiceInstance(request
							.getServiceInstanceId());

			ServiceInstanceBinding binding = new ServiceInstanceBinding(
					bindingId, request.getServiceInstanceId(),
					provider.getCreds(instance), null, appGuid);

			bindings.saveBinding(binding);
			log(bindingId, "Created service binding for app " + appGuid,
					COMPLETE);
			return binding;
		} catch (Exception e) {
			log(bindingId, "Failed to bind app " + appGuid, FAILED);
			throw e;
		}
	}

	private void throwIfCopyAlreadyBoundToApp(String appGuid,
			String serviceInstanceId)
			throws ServiceInstanceBindingExistsException {
		List<ServiceInstanceBinding> boundInstances = bindings
				.getBindings()
				.stream()
				.filter(s -> appGuid.equals(s.getAppGuid())
						&& serviceInstanceId.equals(s.getServiceInstanceId()))
				.collect(Collectors.toList());
		if (0 < boundInstances.size()) {
			throw new ServiceInstanceBindingExistsException(
					boundInstances.get(0));
		}
	}

	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request)
			throws ServiceBrokerException {
		try {
			log(request.getBindingId(), "Removing binding ", IN_PROGRESS);
			ServiceInstanceBinding binding = bindings.removeBinding(request
					.getBindingId());
			log(request.getBindingId(), "Removing binding ", COMPLETE);

			return binding;
		} catch (Exception e) {
			log(request.getBindingId(), "Failed to remove binding ", FAILED);
			throw e;
		}
	}

	public List<InstancePair> getAppToCopyBinding() {
		//@formatter:off
		return bindings.getBindings()
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

	private void throwIfDuplicateBinding(String bindingId)
			throws ServiceInstanceBindingExistsException {
		if (null != bindings.getBinding(bindingId)) {
			throw new ServiceInstanceBindingExistsException(
					bindings.getBinding(bindingId));
		}
	}
}
