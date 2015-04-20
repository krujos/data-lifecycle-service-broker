package io.pivotal.cdm.postgres;

import io.pivotal.cdm.provider.DataProvider;

import java.sql.SQLException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by jkruck on 4/20/15.
 */
public class PostgresDataProvider implements DataProvider {

	@Autowired
	private PostgresScriptExecutor executor;

	@Override
	public void sanitize(String script, Map<String, Object> creds) {
		// We assume that the URI has username, password and db embedded in it.
		checkForURI(creds);

		try {
			executor.execute(script, creds);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void checkForURI(Map<String, Object> creds) {
		if (!creds.containsKey("uri")) {
			throw new IllegalArgumentException("Credentials lack required "
					+ "`uri` parameter");
		}
	}
}
