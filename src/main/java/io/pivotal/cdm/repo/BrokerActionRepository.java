package io.pivotal.cdm.repo;

import io.pivotal.cdm.model.*;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "actions", path = "actions")
public interface BrokerActionRepository extends
		PagingAndSortingRepository<BrokerAction, String> {

	List<BrokerAction> findByState(@Param("state") BrokerActionState state);
}
