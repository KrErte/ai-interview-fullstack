package ee.kerrete.ainterview.billing.checkout;

import org.springframework.http.HttpStatus;

/** A rejected checkout, answered as {@code status {"error": code}}. */
public class CheckoutException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public CheckoutException(HttpStatus status, String code) {
        super(code);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    static CheckoutException billingDisabled() {
        return new CheckoutException(HttpStatus.NOT_FOUND, "billing_disabled");
    }

    static CheckoutException badRequest(String code) {
        return new CheckoutException(HttpStatus.BAD_REQUEST, code);
    }
}
