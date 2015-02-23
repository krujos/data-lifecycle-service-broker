package io.pivotal.cdm.config;

import io.pivotal.cdm.provider.CopyProvider;
import io.pivotal.cdm.repo.BrokerActionRepository;
import io.pivotal.cdm.service.PostgresServiceInstanceService;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;

@Configuration
public class PostgresServiceInstanceServiceConfig {

	@Autowired
	CopyProvider provider;

	@Value("#{environment.SOURCE_INSTANCE_ID}")
	String sourceInstanceId;

	@Autowired
	BrokerActionRepository brokerRepo;

	@Bean
	PostgresServiceInstanceService postgresServiceInstanceService() {
		return new PostgresServiceInstanceService(provider, sourceInstanceId,
				brokerRepo);
	}
}
