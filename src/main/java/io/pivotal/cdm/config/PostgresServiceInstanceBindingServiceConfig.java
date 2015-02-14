package io.pivotal.cdm.config;

import io.pivotal.cdm.service.PostgresServiceInstanceBindingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.ec2.AmazonEC2Client;

@Configuration
public class PostgresServiceInstanceBindingServiceConfig {

	@Autowired
	AmazonEC2Client ec2Client;

	@Bean
	PostgresServiceInstanceBindingService postgresServiceInstanceBindingService() {
		return new PostgresServiceInstanceBindingService(ec2Client);
	}
}
