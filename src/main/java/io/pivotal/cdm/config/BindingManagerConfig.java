package io.pivotal.cdm.config;

import io.pivotal.cdm.repo.BindingRepository;
import io.pivotal.cdm.service.LCServiceInstanceBindingManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BindingManagerConfig {

	@Autowired
	private BindingRepository repo;

	@Bean
	public LCServiceInstanceBindingManager bindingRepo() {
		return new LCServiceInstanceBindingManager(repo);
	}
}
