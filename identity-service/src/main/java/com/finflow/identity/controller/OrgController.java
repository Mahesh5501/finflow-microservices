package com.finflow.identity.controller;

import com.finflow.identity.dto.request.InviteMemberRequest;
import com.finflow.identity.dto.request.CreateOrgRequest;
import com.finflow.identity.dto.request.TokenRequest;
import com.finflow.identity.dto.request.UpdateOrgMemberRoleRequest;
import com.finflow.identity.dto.response.AuditLogResponse;
import com.finflow.identity.dto.response.InviteMemberResponse;
import com.finflow.identity.dto.response.OrgMemberResponse;
import com.finflow.identity.dto.response.OrgResponse;
import com.finflow.identity.security.AuthenticatedUser;
import com.finflow.identity.service.OrgService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Org + membership HTTP API (Bearer access JWT required).
 *
 * <pre>
 * Create/list/get org → invite member → accept invite → list/change/remove → audit
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/orgs")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrgResponse create(Authentication authentication, @Valid @RequestBody CreateOrgRequest request) {
        return orgService.create(caller(authentication).userId(), request.name());
    }

    @GetMapping
    public List<OrgResponse> listMine(Authentication authentication) {
        return orgService.listMine(caller(authentication).userId());
    }

    @GetMapping("/{orgId}")
    public OrgResponse get(Authentication authentication, @PathVariable Long orgId) {
        return orgService.get(caller(authentication).userId(), orgId);
    }

    /** Puts membership in INVITED; invitee must accept with the token. */
    @PostMapping("/{orgId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteMemberResponse inviteMember(
            Authentication authentication,
            @PathVariable Long orgId,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        return orgService.inviteMember(caller(authentication).userId(), orgId, request.email(), request.role());
    }

    /** Logged-in user accepts an invite (email on token must match). */
    @PostMapping("/invites/accept")
    public OrgMemberResponse acceptInvite(
            Authentication authentication,
            @Valid @RequestBody TokenRequest request
    ) {
        return orgService.acceptInvite(caller(authentication).userId(), request.token());
    }

    @GetMapping("/{orgId}/members")
    public List<OrgMemberResponse> listMembers(Authentication authentication, @PathVariable Long orgId) {
        return orgService.listMembers(caller(authentication).userId(), orgId);
    }

    @PutMapping("/{orgId}/members/{userId}/role")
    public OrgMemberResponse changeRole(
            Authentication authentication,
            @PathVariable Long orgId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateOrgMemberRoleRequest request
    ) {
        return orgService.changeRole(caller(authentication).userId(), orgId, userId, request.role());
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            Authentication authentication,
            @PathVariable Long orgId,
            @PathVariable Long userId
    ) {
        orgService.removeMember(caller(authentication).userId(), orgId, userId);
    }

    /** ORG_ADMIN only — who invited / changed roles / removed members. */
    @GetMapping("/{orgId}/audit")
    public List<AuditLogResponse> audit(Authentication authentication, @PathVariable Long orgId) {
        return orgService.listAudit(caller(authentication).userId(), orgId);
    }

    private static AuthenticatedUser caller(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
