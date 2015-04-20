package io.pivotal.cdm.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
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

	// @Mock
	// ScriptUtils scriptUtils;

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

	// @Test
	// public void itShouldExecuteTheScript() {
	//
	// String script = "This is the script;";
	//
	// verify(scriptUtils, times(1)).executeSqlScript(connection,
	// argThat(new ArgumentMatcher<ByteArrayResource>() {
	// @Override
	// public boolean matches(Object argument) {
	// String s = new String((byte[]) argument);
	// return s.equals(script);
	// }
	// }));
	//
	// }
}
