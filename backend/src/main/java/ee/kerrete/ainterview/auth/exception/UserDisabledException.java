package ee.kerrete.ainterview.auth.exception;

/**
 * Exception thrown when a disabled user attempts to authenticate.
 */
public class UserDisabledException extends AuthException {

    public UserDisabledException() {
        super("User is disabled");
    }

    public UserDisabledException(String message) {
        super(message);
    }
}
