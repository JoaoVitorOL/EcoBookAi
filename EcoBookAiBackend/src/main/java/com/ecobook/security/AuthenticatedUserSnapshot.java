package com.ecobook.security;

import com.ecobook.model.enums.Role;

import java.util.UUID;

/**
 * Immutable authenticated user view cached for request-time security checks.
 */
public record AuthenticatedUserSnapshot(
        UUID id,
        String email,
        Role role,
        boolean profileComplete
) {
}
