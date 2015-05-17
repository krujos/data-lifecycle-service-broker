package org.cloudfoundry.community.servicebroker.datalifecycle.config;

import org.cloudfoundry.community.servicebroker.datalifecycle.repo.ServiceInstanceRepo;
import org.cloudfoundry.community.servicebroker.datalifecycle.service.LCServiceInstanceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LCServiceInstanceManagerConfig {

	@Autowired
	private
	ServiceInstanceRepo repo;

	@Bean
	LCServiceInstanceManager newLCServiceInstanceManager() {
		return new LCServiceInstanceManager(repo);
	}
}
