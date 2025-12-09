package ee.kerrete.ainterview.auth.exception;

/**
 * Base exception for authentication-related errors.
 */
public abstract class AuthException extends RuntimeException {

    protected AuthException(String message) {
        super(message);
    }

    protected AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
