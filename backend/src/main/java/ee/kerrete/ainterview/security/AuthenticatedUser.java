package ee.kerrete.ainterview.security;

import ee.kerrete.ainterview.model.UserRole;

/**
 * Immutable representation of the currently authenticated user.
 * Populated from JWT claims - no database lookup required.
 *
 * Used as the principal in Spring Security's Authentication object.
 */
public record AuthenticatedUser(
    Long id,
    String email,
    UserRole role
) {
    /**
     * Check if user has ADMIN role.
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Check if user has CANDIDATE role.
     */
    public boolean isCandidate() {
        return role == UserRole.CANDIDATE;
    }

    /**
     * Check if user has USER role.
     */
    public boolean isUser() {
        return role == UserRole.USER;
    }

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(UserRole requiredRole) {
        return role == requiredRole;
    }

    /**
     * Get role name as string, or null if no role.
     */
    public String roleName() {
        return role != null ? role.name() : null;
    }
}
