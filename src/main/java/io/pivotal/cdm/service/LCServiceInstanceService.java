package io.pivotal.cdm.service;

import static io.pivotal.cdm.config.LCCatalogConfig.COPY;
import static io.pivotal.cdm.config.LCCatalogConfig.PRODUCTION;
import static io.pivotal.cdm.model.BrokerActionState.COMPLETE;
import static io.pivotal.cdm.model.BrokerActionState.FAILED;
import static io.pivotal.cdm.model.BrokerActionState.IN_PROGRESS;
import io.pivotal.cdm.dto.InstancePair;
import io.pivotal.cdm.model.BrokerAction;
import io.pivotal.cdm.model.BrokerActionState;
import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.provider.DataProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerAsyncRequiredException;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.OperationState;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceLastOperation;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Creating a service instance is a no op for the copy operations, we simply
 * assume that the prod instance exists.
 * 
 * @author jkruck
 *
 */
@Service
public class LCServiceInstanceService implements ServiceInstanceService {
	private Logger logger = Logger.getLogger(LCServiceInstanceService.class);

	private LCServiceInstanceManager instanceManager;

	private CopyProvider copyProvider;

	private String sourceInstanceId;

	private BrokerActionRepository brokerRepo;

	private TaskExecutor executor;

	private DataProvider dataProvider;

	private DataProviderService dataProviderService;

	@Autowired
	public LCServiceInstanceService(
			final CopyProvider copyProvider,
			final DataProvider dataProvider,
			@Value("#{environment.SOURCE_INSTANCE_ID}") final String sourceInstanceId,
			final BrokerActionRepository brokerRepo,
			final LCServiceInstanceManager instanceManager,
			final TaskExecutor executor,
			final DataProviderService dataProviderService) {
		this.copyProvider = copyProvider;
		this.dataProvider = dataProvider;
		this.sourceInstanceId = sourceInstanceId;
		this.brokerRepo = brokerRepo;
		this.instanceManager = instanceManager;
		this.executor = executor;
		this.dataProviderService = dataProviderService;
	}

	@Override
	public ServiceInstance createServiceInstance(
			CreateServiceInstanceRequest request)
			throws ServiceInstanceExistsException, ServiceBrokerException,
			ServiceBrokerAsyncRequiredException {

		String id = request.getServiceInstanceId();
		log(id, "Creating service instance", IN_PROGRESS);
		throwIfDuplicate(id);
		throwIfSync(request);

		ServiceInstance instance = null;

		if (PRODUCTION.equals(request.getPlanId())) {
			instance = new ServiceInstance(request).isAsync(false)
					.withLastOperation(
							new ServiceInstanceLastOperation("Provisioned",
									OperationState.SUCCEEDED));
			instanceManager.saveInstance(instance, sourceInstanceId);
		} else {
			instance = new ServiceInstance(request).isAsync(true)
					.withLastOperation(
							new ServiceInstanceLastOperation(
									"Creating instance",
									OperationState.IN_PROGRESS));
			instanceManager.saveInstance(instance, null);
			provision(request, id, instance);
		}
		return instance;
	}

	private void provision(CreateServiceInstanceRequest request, String id,
			ServiceInstance instance) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {

					String copyId = sourceInstanceId;
					copyId = copyProvider.createCopy(sourceInstanceId);
					logger.info("Sanitizing copy " + copyId);
					String script = dataProviderService.getScript();
					Map<String, Object> creds = copyProvider.getCreds(copyId);
					dataProvider.sanitize(script, creds);

					instance.withLastOperation(new ServiceInstanceLastOperation(
							"Provisioned", OperationState.SUCCEEDED));
					instanceManager.saveInstance(instance, copyId);

					log(id, "Created service instance", COMPLETE);
				} catch (Exception e) {
					instance.withLastOperation(new ServiceInstanceLastOperation(
							e.getMessage(), OperationState.FAILED));
					instanceManager.saveInstance(instance, null);
					log(id,
							"Failed to create service instance: "
									+ e.getMessage(), FAILED);
				}
			}
		});
	}

	@Override
	public ServiceInstance deleteServiceInstance(
			DeleteServiceInstanceRequest request)
			throws ServiceBrokerException, ServiceBrokerAsyncRequiredException {
		throwIfSync(request);
		String id = request.getServiceInstanceId();
		log(id, "Deleting service instance", IN_PROGRESS);
		ServiceInstance instance = instanceManager.getInstance(id);
		if (null == instance) {
			log(id, "Service instance not found", FAILED);
			return null;
		}
		String copyId = instanceManager.getCopyIdForInstance(id);

		instanceManager.saveInstance(
				instance.withLastOperation(
						new ServiceInstanceLastOperation("deprovisioning",
								OperationState.IN_PROGRESS)).isAsync(true),
				copyId);

		deProvision(request, id, instance);

		return instance;

	}

	private void deProvision(DeleteServiceInstanceRequest request, String id,
			ServiceInstance instance) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (COPY.equals(request.getPlanId())) {
						copyProvider.deleteCopy(instanceManager
								.getCopyIdForInstance(id));
					}
					log(id, "Deleted service instance", COMPLETE);
					instanceManager.removeInstance(id);
				} catch (ServiceBrokerException e) {
					log(id,
							"Failed to delete service instance: "
									+ e.getMessage(), FAILED);
					instance.withLastOperation(new ServiceInstanceLastOperation(
							"failed to delete", OperationState.FAILED));
					String copyId = instanceManager.getCopyIdForInstance(id);
					instanceManager.saveInstance(instance, copyId);
				}
			}
		});
	}

	@Override
	public ServiceInstance getServiceInstance(String id) {
		return instanceManager.getInstance(id);
	}

	@Override
	public ServiceInstance updateServiceInstance(
			UpdateServiceInstanceRequest request)
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {

		log(request.getServiceInstanceId(),
				"Updating service instance is not supported", FAILED);
		throw new ServiceInstanceUpdateNotSupportedException(
				"Cannot update plan " + request.getPlanId());
	}

	public String getInstanceIdForServiceInstance(String serviceInstanceId) {
		// @formatter:off
		return instanceManager
				.getInstances()
				.stream()
				.filter(s -> s.getRight().getServiceInstanceId()
						.equals(serviceInstanceId)).findFirst().get().getLeft();
		// @formatter:on
	}

	public List<InstancePair> getProvisionedInstances() {
		// @formatter:off
		return instanceManager.getInstances().stream()
				.map(i -> new InstancePair(sourceInstanceId, i.getLeft()))
				.collect(Collectors.toList());
		// @formatter:on
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
		if (null != instanceManager.getInstance(id)) {
			log(id, "Duplicate service instance requested", FAILED);
			throw new ServiceInstanceExistsException(
					instanceManager.getInstance(id));
		}
	}

	private void throwIfSync(CreateServiceInstanceRequest request)
			throws ServiceBrokerAsyncRequiredException {
		if (!request.hasAsyncClient()) {
			throw new ServiceBrokerAsyncRequiredException(
					"Lifecycle broker requires an async CloudController");
		}
	}

	private void throwIfSync(DeleteServiceInstanceRequest request)
			throws ServiceBrokerAsyncRequiredException {
		if (!request.hasAsyncClient()) {
			throw new ServiceBrokerAsyncRequiredException(
					"Lifecycle broker requires an async CloudController");
		}
	}
}
