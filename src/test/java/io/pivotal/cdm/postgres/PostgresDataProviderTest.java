package io.pivotal.cdm.postgres;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import io.pivotal.cdm.provider.exception.DataProviderSanitizationFailedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		dataProvider = new PostgresDataProvider(executor);

	}

	@Test(expected = IllegalArgumentException.class)
	public void itShouldThrowIllegalArgumentForMissingUri()
			throws DataProviderSanitizationFailedException {
		Map<String, Object> creds = new HashMap<>();
		creds.put("username", "username");
		creds.put("password", "password");
		// No URI!
		dataProvider.sanitize("", creds);
	}

	@Test
	public void itShouldExecuteTheScript() throws SQLException,
			DataProviderSanitizationFailedException {
		Map<String, Object> creds = new HashMap<>();
		creds.put("username", "username");
		creds.put("password", "password");
		creds.put("uri", "fake_uri");
		dataProvider.sanitize(script, creds);
		verify(executor, times(1)).execute(anyString(), any());
	}

	@Test(expected = DataProviderSanitizationFailedException.class)
	public void itShouldThrowASanitizeFailedExceptionIfTheScriptBarfs()
			throws Exception {
		doThrow(new SQLException("Broken")).when(executor)
				.execute(any(), any());
		Map<String, Object> creds = new HashMap<>();
		creds.put("username", "username");
		creds.put("password", "password");
		creds.put("uri", "fake_uri");
		dataProvider.sanitize(script, creds);
	}
}
