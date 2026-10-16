package ee.kerrete.ainterview.billing.checkout;

import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FP-61: Stripe session params, built without any network call. */
class StripeCheckoutGatewayTest {

    private static CheckoutGateway.SessionRequest request(String priceId) {
        return new CheckoutGateway.SessionRequest("buyer@example.com", "7", priceId, 1900, "eur", "Roadmap",
                "Roadmap. waiver text", "https://app.example.test/roadmap?paid=1", "https://app.example.test/risk",
                Map.of("purchase_id", "42", "product", "roadmap"));
    }

    @Test
    void priceIdModeSendsThePrice() {
        SessionCreateParams p = StripeCheckoutGateway.toParams(request("price_test_1"));
        assertThat(p.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
        assertThat(p.getLineItems()).hasSize(1);
        assertThat(p.getLineItems().get(0).getPrice()).isEqualTo("price_test_1");
        assertThat(p.getLineItems().get(0).getPriceData()).isNull();
        assertThat(p.getLineItems().get(0).getQuantity()).isEqualTo(1L);
        assertThat(p.getCustomerEmail()).isEqualTo("buyer@example.com");
        assertThat(p.getClientReferenceId()).isEqualTo("7");
        assertThat(p.getMetadata()).containsEntry("purchase_id", "42").containsEntry("product", "roadmap");
        assertThat(p.getPaymentIntentData().getDescription()).isEqualTo("Roadmap. waiver text");
        assertThat(p.getPaymentIntentData().getReceiptEmail()).isEqualTo("buyer@example.com");
        assertThat(p.getInvoiceCreation()).as("no invoice_creation (TEAM_LEAD)").isNull();
        assertThat(p.getSuccessUrl()).isEqualTo("https://app.example.test/roadmap?paid=1");
        assertThat(p.getCancelUrl()).isEqualTo("https://app.example.test/risk");
    }

    @Test
    void withoutPriceIdSendsPriceData() {
        SessionCreateParams.LineItem item = StripeCheckoutGateway.toParams(request(null)).getLineItems().get(0);
        assertThat(item.getPrice()).isNull();
        assertThat(item.getPriceData().getUnitAmount()).isEqualTo(1900L);
        assertThat(item.getPriceData().getCurrency()).isEqualTo("eur");
        assertThat(item.getPriceData().getProductData().getName()).isEqualTo("Roadmap");
    }

    @Test
    void apiVersionIsPinnedToTheSdkVersion() {
        assertThat(StripeCheckoutGateway.API_VERSION).isEqualTo(com.stripe.Stripe.API_VERSION).isNotBlank();
        assertThat(com.stripe.net.RequestOptions.unsafeGetStripeVersionOverride(StripeCheckoutGateway.requestOptions()))
                .isEqualTo(com.stripe.Stripe.API_VERSION);
    }

    @Test
    void missingSecretKeyFailsWithoutCallingStripe() {
        StripeCheckoutGateway gateway = new StripeCheckoutGateway(
                new CheckoutSettings("", 1900, "eur", "http://localhost:4200", 5000, 20000));
        assertThatThrownBy(() -> gateway.createSession(request("price_test_1")))
                .isInstanceOf(CheckoutGatewayException.class)
                .hasMessageNotContaining("buyer@example.com");
    }

    @Test
    void settingsNeverPrintTheKey() {
        CheckoutSettings s = new CheckoutSettings("sk_test_secret", 1900, "EUR", "https://x.test/", 5000, 20000);
        assertThat(s.toString()).doesNotContain("sk_test_secret");
        assertThat(s.currency()).isEqualTo("eur");
        assertThat(s.successUrl()).isEqualTo("https://x.test/roadmap?paid=1");
        assertThat(s.connectTimeoutMs()).isEqualTo(5000);
        assertThat(s.readTimeoutMs()).isEqualTo(20000);
    }
}
