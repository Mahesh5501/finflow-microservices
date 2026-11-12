package com.finflow.identity.auth.application;

import com.finflow.identity.auth.dto.AuthenticationResponse;
import com.finflow.identity.auth.dto.DevTokenResponse;
import com.finflow.identity.organization.dto.OrgResponse;
import com.finflow.identity.auth.dto.RegisterResponse;
import com.finflow.identity.auth.dto.UserProfileResponse;
import com.finflow.identity.organization.domain.OrgRole;
import com.finflow.identity.exception.ApiException;
import com.finflow.identity.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private OrgService orgService;

    @Autowired
    private JwtService jwtService;

    private UserProfileResponse registerVerified(String email, String password) {
        RegisterResponse registered = authService.register(email, password);
        assertNotNull(registered.token());
        authService.verifyEmail(registered.token());
        return registered.profile();
    }

    @Test
    void registerAndLoginIssuesAccessAndRefresh() {
        UserProfileResponse created = registerVerified("demo@finflow.test", "password12");
        assertNotNull(created.userId());

        AuthenticationResponse login = authService.login("demo@finflow.test", "password12");
        assertEquals("Bearer", login.tokenType());
        assertNotNull(login.refreshToken());
        Claims claims = jwtService.parse(login.accessToken());
        assertEquals(String.valueOf(created.userId()), claims.getSubject());
        assertEquals("demo@finflow.test", claims.get("email", String.class));
        assertFalse(jwtService.jwks().get("keys").toString().isBlank());
    }

    @Test
    void loginBlockedUntilEmailVerified() {
        authService.register("pending@finflow.test", "password12");
        ApiException ex = assertThrows(
                ApiException.class,
                () -> authService.login("pending@finflow.test", "password12")
        );
        assertEquals("EMAIL_NOT_VERIFIED", ex.getCode());
    }

    @Test
    void refreshRotatesToken() {
        registerVerified("refresh@finflow.test", "password12");
        AuthenticationResponse login = authService.login("refresh@finflow.test", "password12");
        AuthenticationResponse refreshed = authService.refresh(login.refreshToken());
        assertNotEquals(login.refreshToken(), refreshed.refreshToken());
        ApiException ex = assertThrows(ApiException.class, () -> authService.refresh(login.refreshToken()));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void logoutRevokesRefresh() {
        registerVerified("logout@finflow.test", "password12");
        AuthenticationResponse login = authService.login("logout@finflow.test", "password12");
        authService.logout(login.refreshToken());
        assertThrows(ApiException.class, () -> authService.refresh(login.refreshToken()));
    }

    @Test
    void forgotAndResetPassword() {
        registerVerified("reset@finflow.test", "password12");
        DevTokenResponse forgot = authService.forgotPassword("reset@finflow.test");
        assertNotNull(forgot.token());
        authService.resetPassword(forgot.token(), "newpassword99");
        assertThrows(ApiException.class, () -> authService.login("reset@finflow.test", "password12"));
        AuthenticationResponse login = authService.login("reset@finflow.test", "newpassword99");
        assertNotNull(login.accessToken());
    }

    @Test
    void verifiedUserWithoutOrgGetsConsumerPermissionsInJwt() {
        registerVerified("shopper@finflow.test", "password12");
        AuthenticationResponse login = authService.login("shopper@finflow.test", "password12");
        assertTrue(login.orgIds().isEmpty());
        assertTrue(login.permissions().contains("wallet:write"));
        assertTrue(login.permissions().contains("commerce:write"));
        assertFalse(login.permissions().contains("transfers:create"));
        assertFalse(login.permissions().contains("org:manage"));
    }

    @Test
    void orgMembershipPermissionsAppearInJwt() {
        UserProfileResponse user = registerVerified("admin@finflow.test", "password12");
        OrgResponse org = orgService.create(user.userId(), "Acme Finance");
        assertEquals(OrgRole.ORG_ADMIN, org.myRole());

        AuthenticationResponse login = authService.login("admin@finflow.test", "password12");
        assertTrue(login.orgIds().contains(org.orgId()));
        assertTrue(login.permissions().contains("org:manage"));
        assertTrue(login.permissions().contains("wallet:write"));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        registerVerified("dup@finflow.test", "password12");
        ApiException ex = assertThrows(
                ApiException.class,
                () -> authService.register("dup@finflow.test", "password12")
        );
        assertEquals("EMAIL_ALREADY_EXISTS", ex.getCode());
    }

    @Test
    void loginRejectsWrongPassword() {
        registerVerified("wrong@finflow.test", "password12");
        assertThrows(ApiException.class, () -> authService.login("wrong@finflow.test", "bad-password"));
    }

    @Test
    void emailIsNormalized() {
        registerVerified("Case@Finflow.Test", "password12");
        AuthenticationResponse login = authService.login("case@finflow.test", "password12");
        assertEquals("case@finflow.test", login.email());
    }
    @Test
    void emailNormalizationIsIndependentOfServerLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            UserProfileResponse user = registerVerified("IDENTITY@FINFLOW.TEST", "password12");
            assertEquals("identity@finflow.test", user.email());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

}
