package com.example.localcircle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.localcircle.business.model.Domain.*;
import com.example.localcircle.business.service.RegistryService;
import com.example.localcircle.common.error.DomainException;
import com.example.localcircle.data.StoredProcedureGateway;
import java.sql.DriverManager;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class ServiceIntegrationTest {
  @Autowired RegistryService service;
  @MockitoSpyBean StoredProcedureGateway gateway;

  private long auditCount() throws Exception {
    try (var c =
            DriverManager.getConnection(
                System.getenv().getOrDefault("DB_URL", "jdbc:mysql://127.0.0.1:3307/local_circle"),
                "root",
                System.getenv("MYSQL_ROOT_PASSWORD"));
        var s = c.createStatement();
        var r = s.executeQuery("SELECT COUNT(*) FROM preference_audit")) {
      r.next();
      return r.getLong(1);
    }
  }

  @AfterEach
  void clean() {
    reset(gateway);
    var actor = service.actor(1);
    for (var p : service.preferences(actor)) service.delete(actor, p.preferenceId(), p.version());
  }

  @Test
  void secondTableFailureRollsBackBothWrites() throws Exception {
    var actor = service.actor(1);
    long count = auditCount();
    int rows = service.preferences(actor).size();
    doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalStateException("Injected second operation failure");
            })
        .when(gateway)
        .audit(anyLong(), anyLong(), eq("SAVE"));
    assertThrows(IllegalStateException.class, () -> service.save(actor, new SaveCommand(1, 10, 5)));
    assertEquals(rows, service.preferences(actor).size());
    assertEquals(count, auditCount());
  }

  @Test
  void ownershipSnapshotAndRefresh() {
    var actor = service.actor(1);
    var admin = service.actor(3);
    var product = gateway.product(1, false).orElseThrow();
    assertThrows(DomainException.class, () -> service.save(actor, new SaveCommand(1, 20, 1)));
    assertThrows(DomainException.class, () -> service.save(actor, new SaveCommand(1, 12, 1)));
    var p = service.save(actor, new SaveCommand(1, 10, 5));
    assertThrows(
        DomainException.class, () -> service.preference(service.actor(2), p.preferenceId()));
    try {
      service.updateProduct(
          admin,
          1,
          new ProductCommand(
              product.productName(), 61, product.feeRate(), true, product.version()));
      assertEquals(60, service.preference(actor, p.preferenceId()).priceSnapshot());
      var updated = service.update(actor, p.preferenceId(), p.version(), new SaveCommand(1, 11, 2));
      assertEquals(61, updated.priceSnapshot());
      assertEquals("******1122", updated.maskedAccount());
    } finally {
      var now = gateway.product(1, false).orElseThrow();
      service.updateProduct(
          admin,
          1,
          new ProductCommand(
              product.productName(), product.price(), product.feeRate(), true, now.version()));
    }
  }

  @Test
  void databaseConstraintFailureRollsBackPreference() throws Exception {
    var actor = service.actor(1);
    long count = auditCount();
    int rows = service.preferences(actor).size();
    doAnswer(
            invocation -> {
              invocation.getArguments()[2] = "INVALID";
              return invocation.callRealMethod();
            })
        .when(gateway)
        .audit(anyLong(), anyLong(), eq("SAVE"));
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> service.save(actor, new SaveCommand(1, 10, 5)));
    assertEquals(rows, service.preferences(actor).size());
    assertEquals(count, auditCount());
  }

  private int outcome(Callable<?> c) throws Exception {
    try {
      c.call();
      return 200;
    } catch (DomainException e) {
      return e.status();
    }
  }

  @Test
  void concurrentUpdatesHaveOneWinner() throws Exception {
    var actor = service.actor(1);
    var p = service.save(actor, new SaveCommand(1, 10, 5));
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> work =
          () -> {
            ready.countDown();
            start.await();
            return outcome(
                () ->
                    service.update(
                        actor, p.preferenceId(), p.version(), new SaveCommand(1, 10, 6)));
          };
      var first = pool.submit(work);
      var second = pool.submit(work);
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      var codes =
          java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
      assertTrue(codes.contains(200));
      assertTrue(codes.contains(409));
    }
  }

  @Test
  void updateVersusDeleteHasOnlyOneWinner() throws Exception {
    var actor = service.actor(1);
    var p = service.save(actor, new SaveCommand(1, 10, 5));
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var update =
          pool.submit(
              () -> {
                start.await();
                return outcome(
                    () ->
                        service.update(
                            actor, p.preferenceId(), p.version(), new SaveCommand(1, 10, 6)));
              });
      var delete =
          pool.submit(
              () -> {
                start.await();
                return outcome(
                    () -> {
                      service.delete(actor, p.preferenceId(), p.version());
                      return true;
                    });
              });
      start.countDown();
      int a = update.get(10, TimeUnit.SECONDS), b = delete.get(10, TimeUnit.SECONDS);
      assertTrue((a == 200 && (b == 409 || b == 404)) || (b == 200 && (a == 409 || a == 404)));
    }
  }
}
