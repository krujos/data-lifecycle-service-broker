package io.pivotal.cdm.config;

import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.springframework.context.annotation.*;

@Configuration
public class BrokerAPIVersionConfig {

	@Bean
	public BrokerApiVersion brokerApiVersion() {
		return new BrokerApiVersion("2.4");
	}
}
