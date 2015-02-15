package io.pivotal.cdm.servicebinding;

import static com.jayway.restassured.RestAssured.given;
import io.pivotal.cdm.CdmServiceBrokerApplication;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.specification.RequestSpecification;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CdmServiceBrokerApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
@Category(IntegrationTest.class)
public class ServiceBindingIntegrationTest {

	// TODO DRY w/ Catalog test
	@Value("${local.server.port}")
	private int port;

	@Value("#{environment.SECURITY_USER_NAME}")
	private String username;

	@Value("#{environment.SECURITY_USER_PASSWORD}")
	private String password;

	@Before
	public void setUp() {
		RestAssured.port = port;

		RequestSpecification requestSpecification = new RequestSpecBuilder()
				.addHeader("X-Broker-Api-Version", "2.4").build();
		RestAssured.requestSpecification = requestSpecification;
	}

	@Test
	public void itCreatesAnAMIAndImageAndCleansUp() throws JSONException {
		JSONObject args = new JSONObject();
		args.put("service_id", "postgrescmd");
		args.put("plan_id", "copy");
		args.put("app_guid", "app_guid");

		createServiceInstance();

		given().auth().basic(username, password).and()
				.contentType("application/json").and().body(args.toString())
				.and()
				.put("/v2/service_instances/1234/service_bindings/1234521")
				.then().statusCode(201);

		// TODO This should inspect AWS to verify success.

		given().auth()
				.basic(username, password)
				.delete("/v2/service_instances/1234/service_bindings/1234521?service_id=postgrescdm&plan_id=copy")
				.then().statusCode(200);

	}

	private void createServiceInstance() throws JSONException {
		JSONObject serviceInstance = new JSONObject();
		serviceInstance.put("service_id", "postgrescdm");
		serviceInstance.put("plan_id", "copy");
		serviceInstance.put("organization_guid", "org_guid");
		serviceInstance.put("space_guid", "s_guid");
		given().auth().basic(username, password).and()
				.contentType("application/json").and()
				.body(serviceInstance.toString())
				.put("/v2/service_instances/1234").then().statusCode(201);
	}

	@Test
	public void itShouldBeGoneWhenUnbindingSomethingInvalid()
			throws JSONException {
		createServiceInstance();
		given().auth()
				.basic(username, password)
				.delete("/v2/service_instances/1234/service_bindings/nothing?service_id=postgrescdm&plan_id=copy")
				.then().statusCode(410);
	}
}
