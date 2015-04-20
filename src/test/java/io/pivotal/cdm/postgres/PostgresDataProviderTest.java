package io.pivotal.cdm.postgres;

import static org.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

public class PostgresDataProviderTest {


    @InjectMocks
    DriverManager driveManager;

    PostgresDataProvider dataProvider;

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
        //No URI!
        dataProvider.sanitize("", creds);
    }

    @Test
    public void itShouldGetADriverConnectionWithUserAndPass() {
        when(DriverManager.getConnection())
    }
}
