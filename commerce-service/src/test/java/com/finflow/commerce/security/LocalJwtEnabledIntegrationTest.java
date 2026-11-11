package com.finflow.commerce.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A property override restores JWT enforcement even while the local profile is active. */
@SpringBootTest(properties = "commerce.security.jwt-validation-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalJwtEnabledIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void reEnablingValidationRejectsMissingAndMalformedTokens() throws Exception {
        mvc.perform(get("/api/v1/customer/carts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/customer/carts").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
    }
}
