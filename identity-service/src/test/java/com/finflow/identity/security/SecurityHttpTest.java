package com.finflow.identity.security;

import com.finflow.identity.security.JwtService;
import com.finflow.identity.security.ratelimit.RateLimitService;
import com.finflow.identity.auth.dto.RegisterResponse;
import com.finflow.identity.auth.application.AuthService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real MVC/security/JWT/H2 paths; only the external Redis quota decision is stubbed. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityHttpTest.DeniedController.class)
@Transactional
class SecurityHttpTest {

    @TempDir
    Path temporaryDirectory;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtService jwtService;

    // Quota enforcement needs separate Redis integration tests.
    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void allowQuota() {
        when(rateLimitService.tryConsume(anyString())).thenReturn(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/users/me", "/api/v1/orgs"})
    void protectedReadsRequireBearerToken(String path) throws Exception {
        expectUnauthorized(mvc.perform(get(path)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer not-a-jwt", "Bearer ", "Basic abc"})
    void invalidCredentialsCannotReachProtectedController(String authorization) throws Exception {
        expectUnauthorized(mvc.perform(get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, authorization)));
    }

    @Test
    void tokenSignedByAnUntrustedKeyIsRejected() throws Exception {
        String token = new JwtService(900000, temporaryDirectory.resolve("untrusted.pk8").toString(), true).createAccessToken(1L, "user@finflow.test", List.of(), List.of());
        expectUnauthorized(mvc.perform(get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
    }

    @Test
    void signedTokenWithInvalidSubjectIsRejected() throws Exception {
        // createAccessToken refuses non-positive userIds; craft a still-signed JWT with subject "0"
        // so the filter path (not issuance) is what rejects it.
        RSAPrivateKey privateKey = (RSAPrivateKey) ReflectionTestUtils.getField(jwtService, "privateKey");
        String keyId = (String) ReflectionTestUtils.getField(jwtService, "keyId");
        Instant now = Instant.now();
        String token = Jwts.builder()
                .header().keyId(keyId).and()
                .subject("0")
                .claim("email", "user@finflow.test")
                .claim("orgIds", List.of())
                .claim("permissions", List.of())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        expectUnauthorized(mvc.perform(get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
    }

    @Test
    void validTokenReadsItsOwnProfile() throws Exception {
        RegisterResponse registered = registerVerified();
        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(registered)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("userId").value(registered.profile().userId()))
                .andExpect(jsonPath("email").value("security-http@finflow.test"))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Bearer not-a-jwt"})
    void jwksStaysPublicWithOrWithoutAnInvalidOptionalToken(String authorization) throws Exception {
        mvc.perform(get("/.well-known/jwks.json").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("keys[0].kty").value("RSA"))
                .andExpect(jsonPath("keys[0].d").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "verify-email", "resend-verification", "login",
            "refresh", "logout", "forgot-password", "reset-password"})
    void publicAuthPostsReachValidationWithoutJwt(String action) throws Exception {
        mvc.perform(post("/api/v1/auth/" + action).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("code").value("VALIDATION_FAILED"))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void loginWorksWithoutAnExistingAccessToken() throws Exception {
        registerVerified();
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"security-http@finflow.test\",\"password\":\"password12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("accessToken").value(not(blankOrNullString())))
                .andExpect(jsonPath("tokenType").value("Bearer"));
    }

    @Test
    void publicAllowanceAppliesOnlyToPost() throws Exception {
        expectUnauthorized(mvc.perform(get("/api/v1/auth/login")));
    }

    @Test
    void changePasswordStillRequiresAuthentication() throws Exception {
        expectUnauthorized(mvc.perform(post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON).content("{}")));
    }

    @Test
    void invalidVerificationTokenPreservesDomainErrorCode() throws Exception {
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"not-a-real-verification-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("code").value("INVALID_TOKEN"))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void authenticatedNonMemberPreservesOrganisationForbiddenResponse() throws Exception {
        RegisterResponse registered = registerVerified();
        mvc.perform(get("/api/v1/orgs/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(registered)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("code").value("FORBIDDEN"))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void securityAccessDenialUsesJsonWithoutAChallenge() throws Exception {
        RegisterResponse registered = registerVerified();
        mvc.perform(get("/api/v1/test-security-denial")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(registered)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("code").value("FORBIDDEN"))
                .andExpect(jsonPath("message").value("Access denied"))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    private RegisterResponse registerVerified() {
        RegisterResponse registered = authService.register("security-http@finflow.test", "password12");
        authService.verifyEmail(registered.token());
        return registered;
    }

    private String tokenFor(RegisterResponse registered) {
        return jwtService.createAccessToken(registered.profile().userId(), registered.profile().email(),
                List.of(), List.of());
    }

    private void expectUnauthorized(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("message").value("Valid Bearer access token required"))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    /** Test-only route exercising the filter chain's authenticated access-denial handler. */
    @RestController
    static class DeniedController {
        @GetMapping("/api/v1/test-security-denial")
        void denied() {
            throw new AccessDeniedException("Internal detail must not be exposed");
        }
    }
}
