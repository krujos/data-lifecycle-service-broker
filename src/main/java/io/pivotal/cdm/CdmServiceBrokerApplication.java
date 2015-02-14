package io.pivotal.cdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class CdmServiceBrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CdmServiceBrokerApplication.class, args);
	}
}
