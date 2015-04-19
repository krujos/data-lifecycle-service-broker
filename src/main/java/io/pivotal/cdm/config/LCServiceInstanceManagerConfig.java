package io.pivotal.cdm.config;

import io.pivotal.cdm.repo.ServiceInstanceRepo;
import io.pivotal.cdm.service.LCServiceInstanceManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LCServiceInstanceManagerConfig {

	@Autowired
	ServiceInstanceRepo repo;

	@Bean
	LCServiceInstanceManager newLCServiceInstanceManager() {
		return new LCServiceInstanceManager(repo);
	}
}
