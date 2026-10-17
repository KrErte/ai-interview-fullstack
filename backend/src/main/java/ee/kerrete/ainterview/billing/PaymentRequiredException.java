package ee.kerrete.ainterview.billing;

/** The user lacks the paid entitlement; answered as 402 {"error":"payment_required"}. */
public class PaymentRequiredException extends RuntimeException {

    public PaymentRequiredException() {
        super("payment_required");
    }
}
