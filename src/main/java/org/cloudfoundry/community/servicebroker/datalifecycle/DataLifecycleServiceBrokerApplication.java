package org.cloudfoundry.community.servicebroker.datalifecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DataLifecycleServiceBrokerApplication {

	public static void main(String[] args) {
		SpringApplication
				.run(DataLifecycleServiceBrokerApplication.class, args);
	}
}
