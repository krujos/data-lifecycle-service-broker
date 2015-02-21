package io.pivotal.cdm.config;

import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.service.PostgresServiceInstanceBindingService;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;

@Configuration
public class PostgresServiceInstanceBindingServiceConfig {

	@Autowired
	CopyProvider provider;

	@Value("{environment.SOURCE_INSTANCE_ID}")
	private static String sourceInstanceId;

	@Bean
	PostgresServiceInstanceBindingService postgresServiceInstanceBindingService() {
		return new PostgresServiceInstanceBindingService(provider,
				sourceInstanceId);
	}
}
