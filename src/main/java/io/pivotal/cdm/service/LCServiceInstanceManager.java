package io.pivotal.cdm.service;

import io.pivotal.cdm.model.ServiceInstanceEntity;
import io.pivotal.cdm.repo.ServiceInstanceRepo;

import java.util.*;

import org.apache.commons.lang3.tuple.*;
import org.cloudfoundry.community.servicebroker.model.*;

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
		//@formatter:off
		return new ServiceInstance(
				new CreateServiceInstanceRequest(
						i.getServiceDefinitionId(), 
						i.getPlanGuid(), 
						i.getOrgGuid(),
						i.getSpaceGuid())
					.withServiceInstanceId(i.getServiceInstanceId()))
				.withDashboardUrl(i.getDashboardUrl());
		//@formatter:on
	}
}