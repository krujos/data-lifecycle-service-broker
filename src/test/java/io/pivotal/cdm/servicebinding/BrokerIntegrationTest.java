package io.pivotal.cdm.servicebinding;

import static com.jayway.restassured.RestAssured.given;
import static io.pivotal.cdm.config.LCCatalogConfig.COPY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import io.pivotal.cdm.CdmServiceBrokerApplication;
import io.pivotal.cdm.repo.BindingRepository;

import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.amazonaws.services.ec2.AmazonEC2Client;
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
/**
 * These tests assume an empty VPC with one instance (the prod one) running. 
 * You can set env variables to effect where and how the ec2Client behaves, 
 * see README.md in the project root
 *
 */
public class BrokerIntegrationTest {

	// TODO DRY w/ Catalog test
	@Value("${local.server.port}")
	private int port;

	@Value("#{environment.SECURITY_USER_NAME}")
	private String username;

	@Value("#{environment.SECURITY_USER_PASSWORD}")
	private String password;

	@Autowired
	private AmazonEC2Client ec2Client;

	@Value("#{environment.SOURCE_INSTANCE_ID}")
	private String sourceInstanceId;

	@Autowired
	private BindingRepository bindingRepo;

	@Before
	public void setUp() {
		RestAssured.port = port;

		RequestSpecification requestSpecification = new RequestSpecBuilder()
				.addHeader("X-Broker-Api-Version", "2.4").build();
		RestAssured.requestSpecification = requestSpecification;

		bindingRepo.deleteAll();
	}

	@Test
	public void itHasASourceInstance() {
		given().auth().basic(username, password).get("/api/sourceinstance")
				.then().body("sourceInstance", equalTo(sourceInstanceId));
	}

	@Test
	public void itCreatesAnAMIAndImageAndCleansUp() throws Exception {

		provisionAndBindCopy();

		// Try to delete the wrong thing;
		givenTheBroker()
				.delete("/v2/service_instances/1234/service_bindings/nothing?service_id=lifecycle-sb&plan_id=copy")
				.then().statusCode(410);

		validateBinding();

		unprovisionAndUnbindCopy();

		givenTheBroker()
				.delete("/v2/service_instances/1234?service_id=lifecycle-sb&plan_id=copy&accepts_incomplete=true")
				.then().statusCode(410);

		validateNothingProvisioned();
	}

	private void unprovisionAndUnbindCopy() throws Exception {
		givenTheBroker()
				.delete("/v2/service_instances/1234/service_bindings/1234521?service_id=lifecycle-sb&plan_id=copy")
				.then().statusCode(200);

		validateNoBinding();

		givenTheBroker()
				.delete("/v2/service_instances/1234?service_id=lifecycle-sb&plan_id=copy&accepts_incomplete=true")
				.then().statusCode(202);

		validateNothingProvisioned();
	}

	private void provisionAndBindCopy() throws Exception {
		validateNoBinding();
		validateNothingProvisioned();

		JSONObject serviceInstance = new JSONObject();
		serviceInstance.put("service_id", "lifecycle-sb");
		serviceInstance.put("plan_id", COPY);
		serviceInstance.put("organization_guid", "org_guid");
		serviceInstance.put("space_guid", "s_guid");
		givenTheBroker().and().contentType("application/json").and()
				.body(serviceInstance.toString())
				.put("/v2/service_instances/1234?accepts_incomplete=true")
				.then().statusCode(202);

		validateProvisioning();

		JSONObject args = new JSONObject();
		args.put("service_id", "postgrescmd");
		args.put("plan_id", COPY);
		args.put("app_guid", "app_guid");
		givenTheBroker().and().contentType("application/json").and()
				.content(args.toString()).and()
				.put("/v2/service_instances/1234/service_bindings/1234521")
				.then().statusCode(201);

		validateBinding();
		Thread.sleep(10000); // Let AWS get the machines moving;
	}

	private RequestSpecification givenTheBroker() {
		return given().auth().basic(username, password);
	}

	private void validateNothingProvisioned() throws Exception {
		waitForDeProvisionCompletion();
		//@formatter:off
		givenTheBroker()
			.get("/api/instances")
			.then()
			.body("$", hasSize(0));

		givenTheBroker()
			.get("/v2/service_instances/1234")
			.then()
			.statusCode(410);
		//@formatter:on

	}

	private void validateNoBinding() throws Exception {

		givenTheBroker().get("/api/bindings").then().body("$", hasSize(0));
	}

	private void validateBinding() {
		//@formatter:off
		givenTheBroker()
			.get("/api/bindings")
			.then()
			.body("[0].source", equalTo("app_guid"))
			.and()
			.body("[0].copy", notNullValue())
			.and()
			.body("$", hasSize(1));
		//@formatter:on
	}

	private void validateProvisioning() throws JSONException,
			InterruptedException {

		// Because the provision is async we have to give it some time
		// to happen. We poll here and check it's state.
		//@formatter:off

		givenTheBroker().get("/v2/service_instances/1234")
			.then()
			.body("last_operation.state", equalTo("in progress"))
			.and()
			.statusCode(200);

		waitForProvisionCompletion();
		
		givenTheBroker()
			.get("/api/instances")
			.then()
			.body("[0].source", equalTo(sourceInstanceId))
			.and()
			.body("[0].copy", notNullValue())
			.and()
			.body("$", hasSize(1));
		
		givenTheBroker()
			.get("/v2/service_instances/1234")
			.then()
			.body("last_operation.state", equalTo("succeeded"))
			.and()
			.statusCode(200);
		//@formatter:on
	}

	private void waitForProvisionCompletion() throws InterruptedException {

		int retries = 0;
		do {
			ServiceInstance instance = givenTheBroker()
					.get("/v2/service_instances/1234").andReturn()
					.as(ServiceInstance.class);
			if (!"in progress".equals(instance
					.getServiceInstanceLastOperation().getState())) {
				break;
			}
			Thread.sleep(3000);
			++retries;
		} while (retries != 30); // 90 seconds
	}

	private void waitForDeProvisionCompletion() throws InterruptedException {
		int retries = 0;
		int statusCode = 0;
		do {
			statusCode = givenTheBroker().get("/v2/service_instances/1234")
					.andReturn().statusCode();
			Thread.sleep(3000);
			++retries;
		} while (retries != 30 && 410 != statusCode); // 90 seconds or GONE
	}
}
