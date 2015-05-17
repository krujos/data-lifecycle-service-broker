package org.cloudfoundry.community.servicebroker.datalifecycle.repo;

import java.util.List;

import org.cloudfoundry.community.servicebroker.datalifecycle.model.BrokerAction;
import org.cloudfoundry.community.servicebroker.datalifecycle.model.BrokerActionState;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "actions", path = "actions")
public interface BrokerActionRepository extends
		PagingAndSortingRepository<BrokerAction, String> {

	List<BrokerAction> findByState(@Param("state") BrokerActionState state);
}
