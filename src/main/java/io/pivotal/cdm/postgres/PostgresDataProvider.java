package io.pivotal.cdm.postgres;

import io.pivotal.cdm.provider.DataProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by jkruck on 4/20/15.
 */
public class PostgresDataProvider implements DataProvider {
    @Override
    public void sanitize(String script, Map<String, Object> creds) {
        //We assume that the URI has username, password and db embedded in it.
        checkForURI(creds);

        try {
            Connection connection = DriverManager.getConnection((String) creds.get("url"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkForURI(Map<String, Object> creds) {
        if ( !creds.containsKey("uri")) {
            throw new IllegalArgumentException("Credentials lack required " +
                    "`uri` parameter");
        }
    }
}
