package com.example.localcircle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.localcircle.business.model.PersonalPreference.Input;
import com.example.localcircle.business.service.*;
import com.example.localcircle.data.StoredProcedureGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class PersonalPreferenceIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired PersonalPreferenceService personal;
  @Autowired RegistryService registry;
  @MockitoSpyBean StoredProcedureGateway gateway;
  private static final String H = "X-Demo-User-Id", P = "/api/v1/preferences";

  private Connection admin() throws Exception {
    return DriverManager.getConnection(
        System.getenv("DB_URL"), "root", System.getenv("MYSQL_ROOT_PASSWORD"));
  }

  private long count(String table) throws Exception {
    try (var c = admin();
        var s = c.createStatement();
        var r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
      r.next();
      return r.getLong(1);
    }
  }

  @AfterEach
  void cleanup() throws Exception {
    reset(gateway);
    try (var c = admin();
        var s = c.createStatement()) {
      s.executeUpdate("DELETE FROM preference WHERE user_id=1");
      s.executeUpdate("DELETE FROM account WHERE user_id=1 AND account_id NOT IN(10,11,12)");
    }
  }

  @Test
  void customCrudEncryptionAndOwnership() throws Exception {
    var response =
        mvc.perform(
                post(P)
                    .header(H, 1)
                    .contentType("application/json")
                    .content(
                        """
                        {"productName":"<img src=x onerror=alert(1)>","price":0.1,"feeRate":0.1,"plannedQuantity":3,"accountNumber":"001234567890"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.totalAmount").value(0.33))
            .andExpect(jsonPath("$.maskedAccount").value("******7890"))
            .andExpect(jsonPath("$.accountNumber").doesNotExist())
            .andReturn();
    var node = mapper.readTree(response.getResponse().getContentAsString());
    long id = node.get("preferenceId").asLong(), account = node.get("accountId").asLong();
    mvc.perform(get("/api/v1/accounts/" + account + "/number").header(H, 1))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.accountNumber").value("001234567890"));
    mvc.perform(get("/api/v1/accounts/" + account + "/number").header(H, 2))
        .andExpect(status().isNotFound());
    try (var c = admin();
        var s =
            c.prepareStatement("SELECT number_ciphertext,key_id FROM account WHERE account_id=?")) {
      s.setLong(1, account);
      try (var r = s.executeQuery()) {
        assertTrue(r.next());
        assertNotNull(r.getString(2));
        assertFalse(r.getString(1).contains("001234567890"));
      }
    }
    mvc.perform(
            put(P + "/" + id)
                .header(H, 1)
                .contentType("application/json")
                .content(
                    """
                    {"productName":"My revised plan","price":12.5,"feeRate":0.02,"plannedQuantity":2,"accountId":10,"version":0}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalAmount").value(25.5));
    mvc.perform(delete(P + "/" + id + "?version=0").header(H, 1)).andExpect(status().isConflict());
    mvc.perform(delete(P + "/" + id + "?version=1").header(H, 1)).andExpect(status().isNoContent());
  }

  @Test
  void newAccountAndPreferenceAndAuditRollbackTogether() throws Exception {
    long accounts = count("account"),
        preferences = count("preference"),
        audits = count("preference_audit");
    doAnswer(
            i -> {
              i.callRealMethod();
              throw new IllegalStateException("Injected audit failure");
            })
        .when(gateway)
        .audit(anyLong(), anyLong(), eq("SAVE"));
    assertThrows(
        IllegalStateException.class,
        () ->
            personal.save(
                registry.actor(1),
                new Input(
                    null,
                    null,
                    "000012345678",
                    2,
                    "Private",
                    new BigDecimal("10"),
                    new BigDecimal("0.01"),
                    null)));
    assertEquals(accounts, count("account"));
    assertEquals(preferences, count("preference"));
    assertEquals(audits, count("preference_audit"));
  }

  @Test
  void catalogChangesDoNotRewritePersonalNameOrPrice() {
    var actor = registry.actor(1);
    var original = registry.products().getFirst();
    var saved =
        personal.save(
            actor,
            new Input(
                original.productId(),
                10L,
                null,
                2,
                "My private name",
                new BigDecimal("11"),
                new BigDecimal("0.01"),
                null));
    assertEquals(original, registry.products().getFirst());
    assertEquals("My private name", personal.get(actor, saved.preferenceId()).productName());
    assertEquals(
        0,
        new BigDecimal("11").compareTo(personal.get(actor, saved.preferenceId()).priceSnapshot()));
  }

  @Test
  void competingPersonalUpdatesRollbackLosingAccount() throws Exception {
    var actor = registry.actor(1);
    var saved =
        personal.save(
            actor, new Input(null, 10L, null, 1, "Race", BigDecimal.ONE, BigDecimal.ZERO, null));
    long accounts = count("account");
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> work =
          () -> {
            start.await();
            try {
              personal.update(
                  actor,
                  saved.preferenceId(),
                  new Input(
                      null, null, "000012345678", 2, "Race", BigDecimal.ONE, BigDecimal.ZERO, 0L));
              return 200;
            } catch (com.example.localcircle.common.error.DomainException e) {
              return e.status();
            }
          };
      var a = pool.submit(work);
      var b = pool.submit(work);
      start.countDown();
      var outcomes =
          java.util.List.of(
              a.get(10, java.util.concurrent.TimeUnit.SECONDS),
              b.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertTrue(outcomes.contains(200));
      assertTrue(outcomes.contains(409));
    }
    assertEquals(accounts + 1, count("account"));
  }

  @Test
  void rejectsAmbiguousAccountAndPartialProductWithoutReflectingNumbers() throws Exception {
    for (String body :
        new String[] {
          "{\"productName\":\"Private\",\"accountId\":10,\"plannedQuantity\":1}",
          "{\"productId\":1,\"accountId\":10,\"accountNumber\":\"000012345678\",\"plannedQuantity\":1}",
          "{\"productId\":1,\"accountNumber\":\"123\",\"plannedQuantity\":1}"
        }) {
      String response =
          mvc.perform(post(P).header(H, 1).contentType("application/json").content(body))
              .andExpect(status().isBadRequest())
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertFalse(response.contains("000012345678"));
    }
  }
}
