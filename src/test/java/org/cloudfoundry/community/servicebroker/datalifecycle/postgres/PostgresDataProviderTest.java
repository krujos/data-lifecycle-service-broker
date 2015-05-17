package org.cloudfoundry.community.servicebroker.datalifecycle.postgres;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.datalifecycle.exception.DataProviderSanitizationFailedException;
import org.cloudfoundry.community.servicebroker.datalifecycle.postgres.PostgresDataProvider;
import org.cloudfoundry.community.servicebroker.datalifecycle.postgres.PostgresScriptExecutor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PostgresDataProviderTest {

	PostgresDataProvider dataProvider;

	@Mock
	Connection connection;

	@Mock
	PostgresScriptExecutor executor;

	String script = "This is the script;";

	private Map<String, Object> creds;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		dataProvider = new PostgresDataProvider(executor);
		creds = new HashMap<>();
		creds.put("username", "username");
		creds.put("password", "password");
		creds.put("uri", "fake_uri");
	}

	@Test(expected = IllegalArgumentException.class)
	public void itShouldThrowIllegalArgumentForMissingUri()
			throws DataProviderSanitizationFailedException {
		creds.remove("uri");
		dataProvider.sanitize("some-script", creds);
	}

	@Test
	public void itShouldDoNothingForAZeroLengthScript() throws Exception {
		dataProvider.sanitize("", creds);
		verifyZeroInteractions(executor);
	}

	@Test
	public void itShouldExecuteTheScript() throws SQLException,
			DataProviderSanitizationFailedException {
		dataProvider.sanitize(script, creds);
		verify(executor, times(1)).execute(anyString(), any());
	}

	@Test(expected = DataProviderSanitizationFailedException.class)
	public void itShouldThrowASanitizeFailedExceptionIfTheScriptBarfs()
			throws Exception {
		doThrow(new SQLException("Broken")).when(executor)
				.execute(any(), any());

		dataProvider.sanitize(script, creds);
	}

	@Test
	public void itShouldDoNothingIfTheScriptIsNull() throws Exception {
		dataProvider.sanitize(null, null);
		verifyZeroInteractions(executor);
	}
}
