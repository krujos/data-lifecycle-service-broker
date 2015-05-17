package org.cloudfoundry.community.servicebroker.datalifecycle.postgres;

import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.datalifecycle.exception.DataProviderSanitizationFailedException;
import org.cloudfoundry.community.servicebroker.datalifecycle.provider.DataProvider;

/**
 * Created by jkruck on 4/20/15.
 */
public class PostgresDataProvider implements DataProvider {
	private Logger log = Logger.getLogger(PostgresDataProvider.class);

	private PostgresScriptExecutor executor;

	public PostgresDataProvider(PostgresScriptExecutor executor) {
		this.executor = executor;
	}

	@Override
	public void sanitize(String script, Map<String, Object> creds)
			throws DataProviderSanitizationFailedException {
		// We assume that the URI has username, password and db embedded in it.

		log.info("Running script: " + script + ".");
		if (null == script || 0 == script.length()) {
			return;
		}
		checkForURI(creds);

		try {
			executor.execute(script, creds);
			log.info("Sanitization run complete");
		} catch (SQLException e) {
			log.error("Failed to execute script!");
			throw new DataProviderSanitizationFailedException(e.getMessage());
		}
	}

	private void checkForURI(Map<String, Object> creds) {
		if (!creds.containsKey("uri")) {
			throw new IllegalArgumentException("Credentials lack required "
					+ "`uri` parameter");
		}
	}
}
