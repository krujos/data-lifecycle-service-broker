package org.cloudfoundry.community.servicebroker.datalifecycle.repo;

import org.cloudfoundry.community.servicebroker.datalifecycle.model.SanitizationScript;
import org.springframework.data.repository.CrudRepository;

public interface ScriptRepo extends CrudRepository<SanitizationScript, Long> {

}
