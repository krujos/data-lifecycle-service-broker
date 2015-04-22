package io.pivotal.cdm.config;

import io.pivotal.cdm.utils.HostUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HostUtilsConfig {

	@Bean
	public HostUtils newHostUtils() {
		return new HostUtils();
	}
}
