package io.pivotal.cdm.postgres;

import io.pivotal.cdm.provider.DataProvider;

import java.util.Map;

/**
 * Created by jkruck on 4/20/15.
 */
public class PostgresDataProvider implements DataProvider {
    @Override
    public void sanitize(String script, Map<String, Object> creds) {
        if ( !creds.containsKey("uri")) {
            throw new IllegalArgumentException("Credentials lack required " +
                    "`uri` parameter");
        }
    }
}
