package io.pivotal.cdm.catalog;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import io.pivotal.cdm.CdmServiceBrokerApplication;

import java.util.List;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.specification.RequestSpecification;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CdmServiceBrokerApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
@Category(IntegrationTest.class)
public class CatalogIntegrationTest {

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
	public void theCatalogEndpointShouldReturn404WithBadCreds() {
		given().auth().basic(username, "garbage").when().get("/v2/catalog")
				.then().statusCode(401);
	}

	/**
	 * Make sure you have env variables setup for these tests to work...
	 */
	@Test
	public void theCatalogReturnsSuccessWithGoodCreds() {
		given().auth().basic(username, password).when().get("/v2/catalog")
				.then().statusCode(200);
	}

	@Test
	public void theCatalogReturnsTwoPlans() {
		String json = given().auth().basic(username, password).when()
				.get("/v2/catalog").asString();
		List<String> plans = from(json).get("services[0].plans.id");
		assertThat(plans, hasSize(2));
		assertThat(plans, containsInAnyOrder("prod", "copy"));
	}
}
