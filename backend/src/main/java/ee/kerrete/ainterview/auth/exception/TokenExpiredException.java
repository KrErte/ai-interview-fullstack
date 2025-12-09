package ee.kerrete.ainterview.auth.exception;

/**
 * Exception thrown when a token has expired.
 */
public class TokenExpiredException extends AuthException {

    public TokenExpiredException() {
        super("Token has expired");
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}
