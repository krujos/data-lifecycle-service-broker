package org.cloudfoundry.community.servicebroker.datalifecycle.config;

import org.cloudfoundry.community.servicebroker.datalifecycle.postgres.PostgresDataProvider;
import org.cloudfoundry.community.servicebroker.datalifecycle.postgres.PostgresScriptExecutor;
import org.cloudfoundry.community.servicebroker.datalifecycle.provider.DataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PostgresDataProviderConfig {

	@Bean
	public DataProvider postgresDataProvidr() {
		return new PostgresDataProvider(new PostgresScriptExecutor());
	}
}
