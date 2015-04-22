package io.pivotal.cdm.config;

import io.pivotal.cdm.postgres.PostgresDataProvider;
import io.pivotal.cdm.postgres.PostgresScriptExecutor;
import io.pivotal.cdm.provider.DataProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PostgresDataProviderConfig {

	@Bean
	public DataProvider postgresDataProvidr() {
		return new PostgresDataProvider(new PostgresScriptExecutor());
	}
}
