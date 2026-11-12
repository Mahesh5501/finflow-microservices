package com.finflow.commerce.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Token-free local requests still exercise the real chain, controllers, services and H2 persistence. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalSecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void requestsWithoutTokenOrWithStaleTokenUseConfiguredPrincipal() throws Exception {
        mvc.perform(get("/api/v1/customer/carts"))
                .andExpect(status().isOk()).andExpect(jsonPath("customerUserId").value(1));
        mvc.perform(get("/api/v1/customer/carts").header("Authorization", "Bearer stale-token")
                        .header("X-User-Id", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("customerUserId").value(1));
    }

    @Test
    void merchantOrganisationChecksRemainEnabledInLocalMode() throws Exception {
        mvc.perform(post("/api/v1/merchant/coupons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + UUID.randomUUID() + "\",\"percentOff\":10}"))
                .andExpect(status().isCreated());
    }

    @Test
    void localCartCheckoutConfirmsWithoutIdentityWalletOrInventedReferences() throws Exception {
        String created = mvc.perform(post("/api/v1/merchant/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Local item\",\"unitPrice\":25,\"availableQuantity\":5}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long productId = mapper.readTree(created).path("productId").asLong();
        mvc.perform(delete("/api/v1/customer/carts")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/customer/carts/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isOk());
        String response = mvc.perform(post("/api/v1/customer/carts/checkout")
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\":{\"line1\":\"1 Test Road\",\"city\":\"Hyderabad\",\"postalCode\":\"500001\",\"country\":\"IN\"}}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("orders[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("orders[0].customerUserId").value(1))
                .andExpect(jsonPath("orders[0].paymentWalletId").isEmpty())
                .andExpect(jsonPath("orders[0].paymentTransactionId").isEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode order = mapper.readTree(response).path("orders").get(0);
        long orderId = order.path("orderId").asLong();
        mvc.perform(get("/api/v1/customer/products/" + productId))
                .andExpect(status().isOk()).andExpect(jsonPath("availableQuantity").value(3));
        mvc.perform(post("/api/v1/customer/orders/" + orderId + "/cancel"))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("CANCELLED"));
        mvc.perform(get("/api/v1/customer/products/" + productId))
                .andExpect(status().isOk()).andExpect(jsonPath("availableQuantity").value(5));
    }
}
