package com.example.localcircle;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DataSourceConfigurationTest {
  private String url(MockEnvironment environment) throws Exception {
    var properties = new Properties();
    try (var input = getClass().getResourceAsStream("/application.properties")) {
      assertNotNull(input);
      properties.load(input);
    }
    return environment.resolveRequiredPlaceholders(properties.getProperty("spring.datasource.url"));
  }
  @Test void developmentUsesConfiguredDatabasePort() throws Exception {
    assertTrue(url(new MockEnvironment().withProperty("DB_PORT", "13317"))
        .startsWith("jdbc:mysql://127.0.0.1:13317/local_circle?"));
  }
  @Test void defaultAndExplicitUrlRemainSupported() throws Exception {
    assertTrue(url(new MockEnvironment()).startsWith("jdbc:mysql://127.0.0.1:3307/local_circle?"));
    assertEquals("jdbc:mysql://db:3306/local_circle", url(new MockEnvironment()
        .withProperty("DB_PORT", "13317").withProperty("DB_URL", "jdbc:mysql://db:3306/local_circle")));
  }
}
