package ee.kerrete.ainterview.billing.checkout;

import java.util.Map;

/** The payment provider behind POST /api/billing/checkout (Stripe in {@link StripeCheckoutGateway}; mocked in tests). */
public interface CheckoutGateway {

    /**
     * @param priceId     Stripe Price id; when null, {@code amountCents}/{@code currency} are sent as price_data
     * @param description waiver confirmation, used as PaymentIntent description and invoice footer
     */
    record SessionRequest(String customerEmail, String clientReferenceId, String priceId, int amountCents,
                          String currency, String productName, String description, String successUrl,
                          String cancelUrl, Map<String, String> metadata) {
    }

    record Session(String id, String url) {
    }

    /** @throws CheckoutGatewayException when the provider is unreachable, misconfigured or refuses */
    Session createSession(SessionRequest request);
}
