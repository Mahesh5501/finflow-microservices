package com.finflow.identity.organization.application;

import com.finflow.identity.organization.domain.Membership;
import com.finflow.identity.organization.domain.MembershipStatus;
import com.finflow.identity.organization.domain.Org;
import com.finflow.identity.organization.domain.OrgRole;
import com.finflow.identity.auth.domain.User;
import com.finflow.identity.exception.ApiException;
import com.finflow.identity.integration.mail.MailService;
import com.finflow.identity.organization.persistence.MembershipRepository;
import com.finflow.identity.organization.persistence.OrgInviteTokenRepository;
import com.finflow.identity.organization.persistence.OrgRepository;
import com.finflow.identity.auth.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Batch fetches must preserve access checks, response order and missing-reference errors. */
@ExtendWith(MockitoExtension.class)
class OrgServiceQueryTest {

    @Mock
    private OrgRepository orgRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    private OrgService service;

    @BeforeEach
    void createService() {
        service = new OrgService(orgRepository, membershipRepository, userRepository,
                mock(OrgInviteTokenRepository.class), mock(MailService.class), mock(AuditService.class), 3600000);
    }

    @Test
    void listMinePreservesMembershipOrderWhenBatchRowsAreReturnedInAnotherOrder() {
        Membership first = new Membership(1L, 20L, OrgRole.ORG_ADMIN);
        Membership second = new Membership(1L, 10L, OrgRole.MEMBER);
        when(membershipRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .thenReturn(List.of(first, second));
        when(orgRepository.findAllById(List.of(20L, 10L)))
                .thenReturn(List.of(organisation(10L), organisation(20L)));

        var result = service.listMine(1L);

        assertEquals(List.of(20L, 10L), result.stream().map(response -> response.orgId()).toList());
        assertEquals(OrgRole.ORG_ADMIN, result.getFirst().myRole());
        verify(orgRepository).findAllById(List.of(20L, 10L));
        verify(orgRepository, never()).findById(anyLong());
    }

    @Test
    void emptyMembershipsSkipOrganisationFetch() {
        when(membershipRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE)).thenReturn(List.of());

        assertTrue(service.listMine(1L).isEmpty());
        verifyNoInteractions(orgRepository);
    }

    @Test
    void missingOrganisationStillReturnsNotFound() {
        when(membershipRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .thenReturn(List.of(new Membership(1L, 20L, OrgRole.MEMBER)));
        when(orgRepository.findAllById(List.of(20L))).thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, () -> service.listMine(1L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Org not found: 20", exception.getMessage());
    }

    @Test
    void listMembersKeepsActiveBeforeInvitedAndMatchesEachUserById() {
        Membership admin = authorizeAdmin();
        Membership invited = Membership.invited(20L, 7L, OrgRole.MEMBER);
        when(membershipRepository.findByOrgIdAndStatus(7L, MembershipStatus.ACTIVE)).thenReturn(List.of(admin));
        when(membershipRepository.findByOrgIdAndStatus(7L, MembershipStatus.INVITED)).thenReturn(List.of(invited));
        when(userRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(user(20L), user(10L)));

        var result = service.listMembers(10L, 7L);

        assertEquals(List.of(10L, 20L), result.stream().map(response -> response.userId()).toList());
        assertEquals(List.of(MembershipStatus.ACTIVE, MembershipStatus.INVITED),
                result.stream().map(response -> response.status()).toList());
        verify(userRepository).findAllById(List.of(10L, 20L));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void missingMemberUserStillReturnsNotFound() {
        Membership admin = authorizeAdmin();
        when(membershipRepository.findByOrgIdAndStatus(7L, MembershipStatus.ACTIVE)).thenReturn(List.of(admin));
        when(membershipRepository.findByOrgIdAndStatus(7L, MembershipStatus.INVITED)).thenReturn(List.of());
        when(userRepository.findAllById(List.of(10L))).thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, () -> service.listMembers(10L, 7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found: 10", exception.getMessage());
    }

    @Test
    void nonMemberIsRejectedBeforeLoadingOtherUsers() {
        when(membershipRepository.findByUserIdAndOrgId(10L, 7L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.listMembers(10L, 7L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verifyNoInteractions(userRepository, orgRepository);
    }

    @Test
    void lastAdminProtectionUsesDatabaseCountWithoutLoadingAllMembers() {
        Membership admin = authorizeAdmin();
        when(orgRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(organisation(7L)));
        when(membershipRepository.countByOrgIdAndStatusAndRole(7L, MembershipStatus.ACTIVE, OrgRole.ORG_ADMIN))
                .thenReturn(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.removeMember(10L, 7L, 10L)
        );

        assertEquals("Cannot remove or demote the last ORG_ADMIN", exception.getMessage());
        assertTrue(admin.isActive());
        verify(membershipRepository).countByOrgIdAndStatusAndRole(7L, MembershipStatus.ACTIVE, OrgRole.ORG_ADMIN);
        verify(membershipRepository, never()).findByOrgIdAndStatus(7L, MembershipStatus.ACTIVE);
    }

    private Membership authorizeAdmin() {
        Membership admin = new Membership(10L, 7L, OrgRole.ORG_ADMIN);
        when(membershipRepository.findByUserIdAndOrgId(10L, 7L)).thenReturn(Optional.of(admin));
        when(orgRepository.existsById(7L)).thenReturn(true);
        return admin;
    }

    private Org organisation(Long id) {
        Org organisation = new Org("Organisation " + id);
        ReflectionTestUtils.setField(organisation, "id", id);
        return organisation;
    }

    private User user(Long id) {
        User user = new User("member" + id + "@finflow.test", "fixture-password-hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
