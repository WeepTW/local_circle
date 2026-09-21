package com.example.localcircle;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.password=unused")
@AutoConfigureMockMvc
class NonLocalSecurityTest {
  @Autowired MockMvc mvc;

  @Test
  void rejectsDemoHeaderOutsideLocalProfile() throws Exception {
    mvc.perform(get("/api/v1/preferences").header("X-Demo-User-Id", "1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("DEMO_DISABLED"));
  }
}
