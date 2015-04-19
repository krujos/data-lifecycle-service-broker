package io.pivotal.cdm.repo;

import io.pivotal.cdm.model.SanitizationScript;

import org.springframework.data.repository.CrudRepository;

public interface ScriptRepo extends CrudRepository<SanitizationScript, Long> {

}
