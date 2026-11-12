package com.finflow.identity.organization.application;

import com.finflow.identity.organization.dto.InviteMemberResponse;
import com.finflow.identity.organization.dto.OrgMemberResponse;
import com.finflow.identity.organization.dto.OrgResponse;
import com.finflow.identity.auth.dto.RegisterResponse;
import com.finflow.identity.auth.dto.UserProfileResponse;
import com.finflow.identity.organization.domain.MembershipStatus;
import com.finflow.identity.organization.domain.OrgRole;
import com.finflow.identity.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class OrgServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private OrgService orgService;

    private UserProfileResponse registerVerified(String email) {
        RegisterResponse registered = authService.register(email, "password12");
        authService.verifyEmail(registered.token());
        return registered.profile();
    }

    @Test
    void inviteThenAcceptActivatesMember() {
        UserProfileResponse admin = registerVerified("org-admin@finflow.test");
        UserProfileResponse member = registerVerified("org-member@finflow.test");

        OrgResponse org = orgService.create(admin.userId(), "FinFlow Co");
        InviteMemberResponse invite = orgService.inviteMember(
                admin.userId(), org.orgId(), member.email(), OrgRole.FINANCE_OPERATOR
        );
        assertEquals(MembershipStatus.INVITED,
                orgService.listMembers(admin.userId(), org.orgId()).stream()
                        .filter(m -> m.userId().equals(member.userId()))
                        .findFirst().orElseThrow()
                        .status());

        OrgMemberResponse joined = orgService.acceptInvite(member.userId(), invite.token());
        assertEquals(MembershipStatus.ACTIVE, joined.status());
        assertEquals(1, orgService.listMine(member.userId()).size());
        assertTrue(orgService.listAudit(admin.userId(), org.orgId()).size() >= 2);
    }

    @Test
    void memberCannotInvite() {
        UserProfileResponse admin = registerVerified("boss@finflow.test");
        UserProfileResponse member = registerVerified("worker@finflow.test");
        UserProfileResponse other = registerVerified("other@finflow.test");

        OrgResponse org = orgService.create(admin.userId(), "Locked Org");
        InviteMemberResponse invite = orgService.inviteMember(
                admin.userId(), org.orgId(), member.email(), OrgRole.MEMBER
        );
        orgService.acceptInvite(member.userId(), invite.token());

        assertThrows(
                ApiException.class,
                () -> orgService.inviteMember(member.userId(), org.orgId(), other.email(), OrgRole.MEMBER)
        );
    }

    @Test
    void cannotRemoveLastAdmin() {
        UserProfileResponse admin = registerVerified("solo-admin@finflow.test");
        OrgResponse org = orgService.create(admin.userId(), "Solo Org");
        assertThrows(
                IllegalArgumentException.class,
                () -> orgService.removeMember(admin.userId(), org.orgId(), admin.userId())
        );
    }
}
