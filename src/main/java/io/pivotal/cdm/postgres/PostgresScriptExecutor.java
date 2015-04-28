package io.pivotal.cdm.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public class PostgresScriptExecutor {
	private Logger log = Logger.getLogger(PostgresScriptExecutor.class);

	public void execute(String script, Map<String, Object> creds)
			throws SQLException {
		String username = (String) creds.get("username");
		String password = (String) creds.get("password");
		String uri = "jdbc:" + (String) creds.get("uri");
		log.info("Sanitizing " + uri + " " + " as " + username);
		Connection connection = DriverManager.getConnection(uri, username,
				password);
		ScriptUtils.executeSqlScript(connection,
				new ByteArrayResource(script.getBytes()));
	}
}
