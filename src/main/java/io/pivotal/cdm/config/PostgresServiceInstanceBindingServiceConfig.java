package io.pivotal.cdm.config;

import io.pivotal.cdm.service.PostgresServiceInstanceBindingService;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;

import com.amazonaws.services.ec2.AmazonEC2Client;

@Configuration
public class PostgresServiceInstanceBindingServiceConfig {

	@Autowired
	AmazonEC2Client ec2Client;

	@Value("#{environment.PG_USER}")
	private String pgUsername;

	@Value("#{environment.PG_PASSWORD}")
	private String pgPassword;

	@Value("#{environment.PG_URI}")
	private String pgURI;

	@Bean
	PostgresServiceInstanceBindingService postgresServiceInstanceBindingService() {
		return new PostgresServiceInstanceBindingService(ec2Client, pgUsername,
				pgPassword, pgURI);
	}
}
