package io.pivotal.cdm.postgres;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PostgresDataProviderTest {

	@InjectMocks
	DriverManager driveManager;

	PostgresDataProvider dataProvider;

	@Mock
	Connection connection;

	@InjectMocks
	PostgresScriptExecutor executor;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		dataProvider = new PostgresDataProvider();
	}

	@Test(expected = IllegalArgumentException.class)
	public void itShouldThrowIllegalArgumentForMissingUri() {
		Map<String, Object> creds = new HashMap<>();
		creds.put("username", "username");
		creds.put("password", "password");
		// No URI!
		dataProvider.sanitize("", creds);
	}

	@Test
	public void itShouldExecuteTheScript() throws SQLException {

		String script = "This is the script;";
		verify(executor, times(1)).execute(script, any());

	}
}
