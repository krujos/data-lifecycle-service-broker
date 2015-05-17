package org.cloudfoundry.community.servicebroker.datalifecycle.config;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.cloudfoundry.community.servicebroker.datalifecycle.config.LCCatalogConfig;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.junit.Before;
import org.junit.Test;

public class LCCatalogTest {

	private Catalog pgCat;

	@Before
	public void setUp() {
		pgCat = new LCCatalogConfig().catalog();
	}

	@Test
	public void itShouldHaveOneServiceDefinition() {
		assertThat(pgCat.getServiceDefinitions(), hasSize(1));
	}

	@Test
	public void itShouldHaveTwoPlans() {
		assertThat(pgCat.getServiceDefinitions().get(0).getPlans(), hasSize(2));
	}
}
