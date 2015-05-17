package org.cloudfoundry.community.servicebroker.datalifecycle.config;

import org.cloudfoundry.community.servicebroker.datalifecycle.utils.HostUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HostUtilsConfig {

	@Bean
	public HostUtils newHostUtils() {
		return new HostUtils();
	}
}
