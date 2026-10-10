package ee.kerrete.ainterview.api;

/**
 * A requested user-owned resource does not exist for the authenticated user (FP-25).
 * <p>
 * Mapped to 404 by {@link ApiExceptionHandler}. Use the same exception (and message) whether the
 * resource does not exist at all or belongs to another user, so a client cannot probe for it.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
