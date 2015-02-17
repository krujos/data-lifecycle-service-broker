package io.pivotal.cdm.config;

import java.util.*;

import org.cloudfoundry.community.servicebroker.model.*;
import org.springframework.context.annotation.*;

@Configuration
public class PostgresCatalogConfig {

	ServiceDefinition postgresServiceDefinition;

	public static String PRODUCTION = "prod";
	public static String COPY = "copy";

	public PostgresCatalogConfig() {
		List<Plan> plans = new ArrayList<Plan>();
		plans.add(new Plan(PRODUCTION, "Production", "The production database",
				getProdMetadata()));
		plans.add(new Plan(COPY, "Copy", "Copy of production database",
				getCopyMetadata()));
		postgresServiceDefinition = new ServiceDefinition("postgrescdm",
				"Postgres", "Postgres Running in AWS", true, plans);
	}

	private Map<String, Object> getProdMetadata() {
		return new HashMap<String, Object>();
	}

	private Map<String, Object> getCopyMetadata() {
		return new HashMap<String, Object>();
	}

	@Bean
	public Catalog catalog() {
		return new Catalog(Arrays.asList(postgresServiceDefinition));
	}
}
