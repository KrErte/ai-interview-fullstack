package ee.kerrete.ainterview.billing.checkout;

/** Payment provider failure; answered as 503 payment_provider_unavailable. Messages carry no keys or PII. */
public class CheckoutGatewayException extends RuntimeException {

    public CheckoutGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
