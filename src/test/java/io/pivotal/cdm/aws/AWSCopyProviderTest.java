package io.pivotal.cdm.aws;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.ec2.AmazonEC2Client;

public class AWSCopyProviderTest {

	@Mock
	private AWSHelper aws;

	private AWSCopyProvider provider;

	@Mock
	private AmazonEC2Client ec2Client;

	private String pgUser = "pgUser";
	private String pgPass = "pgPass";
	private String pgURI = "postgres://pgUser:pgPass@10.10.10.10:5432/testdb";

	@Before
	public void setUp() throws ServiceBrokerException, TimeoutException {
		MockitoAnnotations.initMocks(this);
		// TODO, need to get the aws helper in there.
		provider = new AWSCopyProvider(aws, pgUser, pgPass, pgURI,
				"sourceInstance");
		// TODO remove the description.......
		when(
				aws.createAMI("sourceInstance",
						"CF Service Broker Snapshot Image")).thenReturn(
				"test_ami");

		when(aws.startEC2Instance("test_ami")).thenReturn("test_instance");

		assertThat("test_instance",
				is(equalTo(provider.createCopy("sourceInstance"))));
	}

	@Test
	public void itShouldCreateAnAMIAndStartAnEC2InstanceForACopy() {
		// / err... it's all done in before...
	}

	@Test
	public void itShouldReturnTheProductionInstanceCredentials()
			throws ServiceBrokerException {
		when(aws.getEC2InstanceIp("sourceInstance")).thenReturn("10.10.10.10");

		Map<String, Object> creds = provider.getCreds("sourceInstance");
		assertThat("pgUser", is(equalTo(creds.get("username"))));
		assertThat("pgPass", is(equalTo(creds.get("password"))));
		assertThat(pgURI, is(equalTo(creds.get("uri"))));
	}

	@Test
	public void itShouldReturnTheRightURIForATestInstance()
			throws ServiceBrokerException {
		when(aws.getEC2InstanceIp("test_instance")).thenReturn("2.2.2.2");
		Map<String, Object> creds = provider.getCreds("test_instance");
		assertThat("postgres://pgUser:pgPass@2.2.2.2:5432/testdb",
				is(equalTo(creds.get("uri"))));
	}

	@Test
	public void itShouldCleanUpWhenDeletingTheCopy()
			throws ServiceBrokerException {

		provider.deleteCopy("test_instance");
		verify(aws).terminateEc2Instance("test_instance");
		verify(aws).deregisterAMI("test_ami");
		verify(aws).deleteStorageArtifacts("test_ami");
	}

	@Test
	public void itShoudlReturnNullForUnknownInstanceCreds()
			throws ServiceBrokerException {
		assertNull(provider.getCreds("wut"));
	}

	@Test
	public void itShouldReturnNullForDeletedInstanceCreds()
			throws ServiceBrokerException {
		itShouldCleanUpWhenDeletingTheCopy();
		assertNull(provider.getCreds("test_instance"));

	}

	@Test(expected = ServiceBrokerException.class)
	public void itWrapsAWSHelperExceptions() throws TimeoutException,
			ServiceBrokerException {
		when(aws.createAMI(any(), any())).thenThrow(new TimeoutException());
		provider.createCopy("sourceInstance");

	}
}
