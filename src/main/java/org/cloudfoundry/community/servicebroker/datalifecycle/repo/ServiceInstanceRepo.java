package org.cloudfoundry.community.servicebroker.datalifecycle.repo;

import org.cloudfoundry.community.servicebroker.datalifecycle.model.ServiceInstanceEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ServiceInstanceRepo extends
		PagingAndSortingRepository<ServiceInstanceEntity, String> {
}
