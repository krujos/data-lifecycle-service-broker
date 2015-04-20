package io.pivotal.cdm.service;

import io.pivotal.cdm.model.ServiceInstanceEntity;
import io.pivotal.cdm.repo.ServiceInstanceRepo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.OperationState;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceLastOperation;

public class LCServiceInstanceManager {
	private ServiceInstanceRepo repo;

	public LCServiceInstanceManager(ServiceInstanceRepo repo) {
		this.repo = repo;
	}

	public ServiceInstance getInstance(String id) {
		ServiceInstanceEntity entity = repo.findOne(id);
		return convert(entity);
	}

	public String getCopyIdForInstance(String id) {
		ServiceInstanceEntity entity = repo.findOne(id);
		return null == entity ? null : entity.getCopyId();
	}

	// TODO this could have a more natural data structure.
	public Collection<Pair<String, ServiceInstance>> getInstances() {
		List<Pair<String, ServiceInstance>> instancePairs = new ArrayList<Pair<String, ServiceInstance>>();
		repo.findAll().forEach(
				e -> instancePairs
						.add(new ImmutablePair<String, ServiceInstance>(e
								.getCopyId(), convert(e))));
		return instancePairs;
	}

	public void saveInstance(ServiceInstance instance, String copyId) {
		repo.save(new ServiceInstanceEntity(instance, copyId));
	}

	public ServiceInstance removeInstance(String id) {
		ServiceInstanceEntity entity = repo.findOne(id);
		if (null != entity) {
			repo.delete(id);
		}
		return convert(entity);
	}

	private ServiceInstance convert(ServiceInstanceEntity i) {
		if (null == i) {
			return null;
		}
		// TODO Gross, the base should handle this for us.
		OperationState state = null;
		switch (i.getLastOperationState()) {
		case "in progress":
			state = OperationState.IN_PROGRESS;
			break;
		case "succeeded":
			state = OperationState.SUCCEEDED;
			break;
		case "failed":
			state = OperationState.FAILED;
			break;
		default:
			assert (false);
		}
		// @formatter:off
		return new ServiceInstance(new CreateServiceInstanceRequest(
				i.getServiceDefinitionId(), i.getPlanGuid(), i.getOrgGuid(),
				i.getSpaceGuid(), true).withServiceInstanceId(i
				.getServiceInstanceId()))
				.withDashboardUrl(i.getDashboardUrl())
				.and()
				.withLastOperation(
						new ServiceInstanceLastOperation(i
								.getLastOperationDescription(), state));
		// @formatter:on
	}
}