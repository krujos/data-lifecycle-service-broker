package io.pivotal.cdm.service;

import io.pivotal.cdm.CdmServiceBrokerApplication;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CdmServiceBrokerApplication.class)
@WebAppConfiguration
@Category(IntegrationTest.class)
public class CdmServiceBrokerApplicationTest {

	@Test
	public void contextLoads() {
	}

}
