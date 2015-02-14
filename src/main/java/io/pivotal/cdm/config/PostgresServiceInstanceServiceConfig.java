package io.pivotal.cdm.config;

import io.pivotal.cdm.service.PostgresServiceInstanceService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostgresServiceInstanceServiceConfig {

	@Bean
	PostgresServiceInstanceService postgresServiceInstanceService() {
		return new PostgresServiceInstanceService();
	}
}
