package com.example.localcircle;

import static org.junit.jupiter.api.Assertions.*;

import com.example.localcircle.business.calculator.DoubleFeeCalculator;
import com.example.localcircle.business.model.Domain.*;
import com.example.localcircle.business.port.RegistryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@Transactional
class GatewayIntegrationTest {
  @Autowired RegistryRepository repository;
  @Autowired javax.sql.DataSource dataSource;

  @Test
  void applicationCannotBypassProcedures() throws Exception {
    try (var c = dataSource.getConnection();
        var s = c.createStatement()) {
      assertThrows(java.sql.SQLException.class, () -> s.executeQuery("SELECT * FROM app_user"));
      assertThrows(
          java.sql.SQLException.class, () -> s.executeUpdate("UPDATE product SET price=0"));
    }
  }

  @Test
  void readsAndWritesViaRealMySqlProcedures() {
    assertEquals(3, repository.products(false).size());
    assertEquals(2, repository.accounts(1).size());
    assertTrue(repository.actor(999).isEmpty());
    assertTrue(repository.account(20, 1).isEmpty());
    assertTrue(repository.account(12, 1).isEmpty());
    var p = repository.product(1, true).orElseThrow();
    var c = new SaveCommand(1, 10, 5);
    var a = DoubleFeeCalculator.calculate(p.price(), p.feeRate(), 5);
    long id = repository.insert(1, c, p, a);
    repository.audit(1, id, "SAVE");
    assertEquals("******9666", repository.preference(id, 1).orElseThrow().maskedAccount());
    assertEquals(300.3, repository.preference(id, 1).orElseThrow().totalAmount());
    assertTrue(repository.preference(id, 2).isEmpty());
    assertEquals(0, repository.delete(id, 2, 0));
    assertEquals(1, repository.update(id, 1, 0, c, p, a));
    assertEquals(0, repository.update(id, 1, 0, c, p, a));
    assertEquals(1, repository.delete(id, 1, 1));
  }

  @Test
  void productAclAndSqlInjectionRemainData() {
    var p = repository.product(1, false).orElseThrow();
    var c =
        new ProductCommand("x'); DROP TABLE product;--", p.price(), p.feeRate(), true, p.version());
    assertEquals(0, repository.updateProduct(1, 1, c));
    assertEquals(0, repository.updateProduct(1, 4, c));
    assertEquals(1, repository.updateProduct(1, 3, c));
    assertEquals(c.productName(), repository.product(1, false).orElseThrow().productName());
    assertEquals(3, repository.products(false).size());
  }
}
