package org.cloudfoundry.community.servicebroker.datalifecycle.config;

import org.cloudfoundry.community.servicebroker.datalifecycle.repo.BindingRepository;
import org.cloudfoundry.community.servicebroker.datalifecycle.service.LCServiceInstanceBindingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BindingManagerConfig {

	@Autowired
	private BindingRepository repo;

	@Bean
	public LCServiceInstanceBindingManager bindingRepo() {
		return new LCServiceInstanceBindingManager(repo);
	}
}
