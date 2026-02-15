package com.innercircle.sacco.reporting.security;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReportingAuthHelper {

    private final JdbcTemplate jdbcTemplate;

    public ReportingAuthHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID resolveCurrentMemberId(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new AccessDeniedException("No email claim in token");
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM members WHERE email = ?",
                    UUID.class,
                    email
            );
        } catch (EmptyResultDataAccessException e) {
            throw new AccessDeniedException("Member not found for authenticated user");
        }
    }

    public void assertAccessToMember(UUID requestedMemberId, Authentication auth) {
        if (hasPrivilegedRole(auth)) {
            return;
        }
        UUID currentMemberId = resolveCurrentMemberId(auth);
        if (!requestedMemberId.equals(currentMemberId)) {
            throw new AccessDeniedException("You do not have access to this member's data");
        }
    }

    private boolean hasPrivilegedRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_TREASURER"));
    }
}
