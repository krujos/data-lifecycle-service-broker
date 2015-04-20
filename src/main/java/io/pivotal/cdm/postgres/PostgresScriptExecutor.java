package io.pivotal.cdm.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

public class PostgresScriptExecutor {

	public void execute(String script, Map<String, Object> creds)
			throws SQLException {
		Connection connection = DriverManager.getConnection((String) creds
				.get("url"));
		ScriptUtils.executeSqlScript(connection,
				new ByteArrayResource(script.getBytes()));
	}
}
