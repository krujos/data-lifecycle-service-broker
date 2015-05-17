package org.cloudfoundry.community.servicebroker.datalifecycle.service;

import org.cloudfoundry.community.servicebroker.datalifecycle.DataLifecycleServiceBrokerApplication;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DataLifecycleServiceBrokerApplication.class)
@WebAppConfiguration
@Category(IntegrationTest.class)
public class CdmServiceBrokerApplicationTest {

	@Test
	public void contextLoads() {
	}

}
