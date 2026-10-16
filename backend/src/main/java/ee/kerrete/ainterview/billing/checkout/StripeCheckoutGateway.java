package ee.kerrete.ainterview.billing.checkout;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stripe Checkout (FP-61). The client is built lazily from STRIPE_SECRET_KEY with the configured timeouts and no
 * automatic retries (the user can retry; a retried create would open a second session).
 */
@Component
public class StripeCheckoutGateway implements CheckoutGateway {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutGateway.class);

    private final CheckoutSettings settings;
    private volatile StripeClient client;

    public StripeCheckoutGateway(CheckoutSettings settings) {
        this.settings = settings;
    }

    @Override
    public Session createSession(SessionRequest request) {
        if (!settings.hasSecretKey()) {
            throw new CheckoutGatewayException("STRIPE_SECRET_KEY is not set", null);
        }
        try {
            com.stripe.model.checkout.Session s = client().v1().checkout().sessions().create(toParams(request));
            return new Session(s.getId(), s.getUrl());
        } catch (StripeException e) {
            // type/code/request id only: Stripe messages can echo request params (the email)
            log.warn("Stripe checkout session failed: {} code={} requestId={}",
                    e.getClass().getSimpleName(), e.getCode(), e.getRequestId());
            throw new CheckoutGatewayException("Stripe checkout session failed", e);
        } catch (RuntimeException e) {
            log.warn("Stripe checkout session failed: {}", e.getClass().getSimpleName());
            throw new CheckoutGatewayException("Stripe checkout session failed", e);
        }
    }

    private StripeClient client() {
        StripeClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    client = StripeClient.builder()
                            .setApiKey(settings.secretKey())
                            .setConnectTimeout(settings.connectTimeoutMs())
                            .setReadTimeout(settings.readTimeoutMs())
                            .setMaxNetworkRetries(0)
                            .build();
                }
                c = client;
            }
        }
        return c;
    }

    /**
     * mode=payment; Stripe Price when set, else price_data. invoice_creation (allowed only with mode=payment) makes
     * Stripe issue an invoice/receipt whose footer repeats the waiver confirmation.
     */
    static SessionCreateParams toParams(SessionRequest r) {
        SessionCreateParams.LineItem.Builder item = SessionCreateParams.LineItem.builder().setQuantity(1L);
        if (r.priceId() != null) {
            item.setPrice(r.priceId());
        } else {
            item.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(r.currency())
                    .setUnitAmount((long) r.amountCents())
                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(r.productName()).build())
                    .build());
        }
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addLineItem(item.build())
                .setCustomerEmail(r.customerEmail())
                .setClientReferenceId(r.clientReferenceId())
                .putAllMetadata(r.metadata())
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .setDescription(r.description())
                        .putAllMetadata(r.metadata())
                        .build())
                .setInvoiceCreation(SessionCreateParams.InvoiceCreation.builder()
                        .setEnabled(true)
                        .setInvoiceData(SessionCreateParams.InvoiceCreation.InvoiceData.builder()
                                .setFooter(r.description())
                                .putAllMetadata(r.metadata())
                                .build())
                        .build())
                .setSuccessUrl(r.successUrl())
                .setCancelUrl(r.cancelUrl())
                .build();
    }
}
