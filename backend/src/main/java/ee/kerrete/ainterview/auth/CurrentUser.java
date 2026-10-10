package ee.kerrete.ainterview.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves the email of the authenticated user from the security context.
 * <p>
 * The JWT filter stores the user's email as the principal, so {@link Authentication#getName()}
 * is the email. User-owned data must always be looked up with this email, never with an
 * email supplied in the request body, query parameters or path.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * @return the authenticated user's email
     * @throws ResponseStatusException 401 if there is no authenticated user
     */
    public static String email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || auth.getName() == null
                || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return auth.getName();
    }

    /**
     * For legacy endpoints that still carry an email in the path: only the user's own
     * email is accepted.
     *
     * @return the authenticated user's email
     * @throws ResponseStatusException 403 if {@code requestedEmail} belongs to someone else
     */
    public static String requireSelf(String requestedEmail) {
        String email = email();
        if (requestedEmail == null || !requestedEmail.trim().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to another user's data is not allowed");
        }
        return email;
    }
}
