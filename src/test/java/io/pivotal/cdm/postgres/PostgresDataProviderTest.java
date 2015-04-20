package io.pivotal.cdm.postgres;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class PostgresDataProviderTest {

    @Test(expected = IllegalArgumentException.class)
    public void itShouldThrowIllegalArgumentForMissingUri() {
        PostgresDataProvider dataProvider = new PostgresDataProvider();
        Map<String, Object> creds = new HashMap<>();
        creds.put("username", "username");
        creds.put("password", "password");
        //No URI!
        dataProvider.sanitize("", creds);
    }
}
