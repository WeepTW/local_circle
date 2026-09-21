package com.example.localcircle;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  private static final String H = "X-Demo-User-Id", P = "/api/v1/preferences";

  @Test
  void crudAndOptimisticErrors() throws Exception {
    var response =
        mvc.perform(
                post(P)
                    .header(H, 1)
                    .contentType("application/json")
                    .content("{\"productId\":1,\"accountId\":10,\"plannedQuantity\":5}"))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.totalAmount").value(300.3))
            .andReturn();
    long id =
        mapper.readTree(response.getResponse().getContentAsString()).get("preferenceId").asLong();
    mvc.perform(get(P + "/" + id).header(H, 2)).andExpect(status().isNotFound());
    mvc.perform(
            put(P + "/" + id)
                .header(H, 1)
                .contentType("application/json")
                .content("{\"productId\":2,\"accountId\":11,\"plannedQuantity\":3,\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.totalAmount").value(540.54));
    mvc.perform(delete(P + "/" + id + "?version=0").header(H, 1)).andExpect(status().isConflict());
    mvc.perform(delete(P + "/" + id + "?version=1").header(H, 1)).andExpect(status().isNoContent());
  }

  @Test
  void strictDtoRejectsMassAssignmentAndMissingValues() throws Exception {
    for (String json :
        new String[] {
          "{}",
          "{\"productId\":1,\"accountId\":10,\"plannedQuantity\":1.5}",
          "{\"productId\":1.5,\"accountId\":10,\"plannedQuantity\":1}",
          "{\"productId\":1,\"accountId\":10,\"plannedQuantity\":0}",
          "{\"productId\":1,\"accountId\":10,\"plannedQuantity\":5,\"userId\":2}",
          "{\"productId\":1,\"accountId\":10,\"plannedQuantity\":5,\"accountNumber\":\"123\"}"
        })
      mvc.perform(post(P).header(H, 1).contentType("application/json").content(json))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType("application/problem+json"))
          .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void identityCorsAndAcl() throws Exception {
    mvc.perform(get(P)).andExpect(status().isUnauthorized());
    mvc.perform(get(P).header(H, "1 OR 1=1")).andExpect(status().isUnauthorized());
    mvc.perform(get(P).header(H, 1).header("Origin", "https://evil.example"))
        .andExpect(status().isForbidden());
    mvc.perform(options(P).header("Origin", "http://localhost:5173"))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    for (int user : new int[] {1, 4})
      mvc.perform(
              patch("/api/v1/admin/products/1")
                  .header(H, user)
                  .contentType("application/json")
                  .content(
                      "{\"productName\":\"test\",\"price\":60,\"feeRate\":0.001,\"active\":true,\"version\":0}"))
          .andExpect(status().isForbidden());
  }

  @Test
  void accountPrivacyAndIdor() throws Exception {
    mvc.perform(get("/api/v1/accounts").header(H, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].maskedAccount").value("******9666"))
        .andExpect(jsonPath("$[0].account_ref").doesNotExist());
    mvc.perform(
            post(P)
                .header(H, 1)
                .contentType("application/json")
                .content("{\"productId\":1,\"accountId\":20,\"plannedQuantity\":5}"))
        .andExpect(status().isNotFound());
  }
}
