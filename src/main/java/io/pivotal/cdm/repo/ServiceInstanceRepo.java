package io.pivotal.cdm.repo;

import io.pivotal.cdm.model.ServiceInstanceEntity;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ServiceInstanceRepo extends
		PagingAndSortingRepository<ServiceInstanceEntity, String> {

}
