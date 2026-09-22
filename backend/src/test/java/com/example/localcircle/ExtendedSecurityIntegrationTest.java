package com.example.localcircle;

import static org.junit.jupiter.api.Assertions.*;
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
class ExtendedSecurityIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  static final String H = "X-Demo-User-Id", P = "/api/v1/preferences";
  static final String SAVE = "{\"productId\":1,\"accountId\":10,\"plannedQuantity\":5}";

  @Test void rejectsAmbiguousAndMalformedIdentity() throws Exception {
    for (String h : new String[]{"0", "-1", "1,3", " 1", "1 OR 1=1", "9999999999999999999"})
      mvc.perform(get(P).header(H, h)).andExpect(status().isUnauthorized());
    mvc.perform(get(P).header(H, "1", "3")).andExpect(status().isUnauthorized());
  }

  @Test void crossUserReadUpdateDeleteCannotModifyVictim() throws Exception {
    var created = mvc.perform(post(P).header(H, 1).contentType("application/json").content(SAVE))
      .andExpect(status().isCreated()).andReturn().getResponse();
    long id = mapper.readTree(created.getContentAsString()).get("preferenceId").asLong();
    mvc.perform(get(P + "/" + id).header(H, 2)).andExpect(status().isNotFound());
    mvc.perform(put(P + "/" + id).header(H, 2).contentType("application/json")
      .content("{\"productId\":1,\"accountId\":20,\"plannedQuantity\":6,\"version\":0}"))
      .andExpect(status().isNotFound());
    mvc.perform(delete(P + "/" + id + "?version=0").header(H, 2)).andExpect(status().isNotFound());
    mvc.perform(get(P + "/" + id).header(H, 1)).andExpect(status().isOk())
      .andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.totalAmount").value(300.3));
  }

  @Test void errorsDoNotReflectPayloadOrImplementationDetails() throws Exception {
    for (String input : new String[]{"{broken:SECRET_SENTINEL}",
      "{\"productId\":1,\"accountId\":10,\"plannedQuantity\":5,\"role\":\"ADMIN\"}",
      "{\"productId\":1,\"accountId\":10,\"plannedQuantity\":9999999999999999999999999}"}) {
      var response = mvc.perform(post(P).header(H, 1).contentType("application/json").content(input))
        .andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.traceId").exists()).andReturn().getResponse().getContentAsString();
      for (String secret : new String[]{"SECRET_SENTINEL", "java.", "SQLException", "jdbc:", "stackTrace", "MYSQL_PASSWORD"})
        assertFalse(response.contains(secret));
    }
  }

  @Test void hostileOriginsCannotPreflightOrWrite() throws Exception {
    for (String origin : new String[]{"null", "https://evil.example", "http://localhost:5173.evil.example"}) {
      mvc.perform(options(P).header("Origin", origin).header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
      mvc.perform(post(P).header(H, 1).header("Origin", origin).contentType("application/json").content(SAVE))
        .andExpect(status().isForbidden());
    }
  }

  @Test void unsupportedContentAndMethodsAreClientErrors() throws Exception {
    mvc.perform(post(P).header(H, 1).contentType("text/plain").content(SAVE)).andExpect(status().isUnsupportedMediaType());
    mvc.perform(patch(P).header(H, 1).contentType("application/json").content(SAVE)).andExpect(status().isMethodNotAllowed());
  }
}
