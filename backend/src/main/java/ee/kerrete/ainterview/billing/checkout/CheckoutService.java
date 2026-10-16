package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.billing.BillingSettings;
import ee.kerrete.ainterview.billing.EntitlementService;
import ee.kerrete.ainterview.billing.Feature;
import ee.kerrete.ainterview.billing.Purchase;
import ee.kerrete.ainterview.billing.PurchaseRepository;
import ee.kerrete.ainterview.billing.PurchaseStatus;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * POST /api/billing/checkout (FP-61). Order: billing on, product, waiver, waiver version, already entitled; then a
 * pending purchase row (reused per user+product), then the Stripe session. Nothing here unlocks anything: only the
 * webhook (FP-62) marks a purchase paid.
 */
@Service
public class CheckoutService {

    private final BillingSettings billing;
    private final CheckoutSettings settings;
    private final EntitlementService entitlements;
    private final AppUserRepository users;
    private final PurchaseRepository purchases;
    private final CheckoutGateway gateway;

    public CheckoutService(BillingSettings billing, CheckoutSettings settings, EntitlementService entitlements,
                           AppUserRepository users, PurchaseRepository purchases, CheckoutGateway gateway) {
        this.billing = billing;
        this.settings = settings;
        this.entitlements = entitlements;
        this.users = users;
        this.purchases = purchases;
        this.gateway = gateway;
    }

    public record CheckoutRequest(String product, Boolean withdrawalWaiverAccepted, String withdrawalWaiverVersion) {
    }

    public record CheckoutResponse(String url) {
    }

    /** @param email the authenticated principal ({@code CurrentUser.email()}) */
    public CheckoutResponse checkout(String email, CheckoutRequest request) {
        if (!billing.enabled()) {
            throw CheckoutException.billingDisabled();
        }
        Feature feature = Arrays.stream(Feature.values())
                .filter(f -> request != null && f.product().equals(request.product()))
                .findFirst()
                .orElseThrow(() -> CheckoutException.badRequest("unknown_product"));
        if (!Boolean.TRUE.equals(request.withdrawalWaiverAccepted())) {
            throw CheckoutException.badRequest("waiver_required");
        }
        String version = request.withdrawalWaiverVersion();
        if (!BillingSettings.CURRENT_WAIVER_VERSION.equals(version)) {
            throw CheckoutException.badRequest("invalid_waiver_version");
        }
        String confirmation = WithdrawalWaiver.confirmation(version)
                .orElseThrow(() -> CheckoutException.badRequest("invalid_waiver_version"));
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new CheckoutException(HttpStatus.UNAUTHORIZED, "unauthorized"));
        if (!entitlements.isLocked(feature, email)) {
            throw new CheckoutException(HttpStatus.CONFLICT, "already_paid");
        }

        String priceId = billing.stripePriceId().isEmpty() ? null : billing.stripePriceId();
        LocalDateTime now = LocalDateTime.now();
        Purchase purchase = purchases
                .findFirstByUserIdAndProductAndStatusOrderByIdDesc(user.getId(), feature.product(), PurchaseStatus.PENDING)
                .orElseGet(Purchase::new);
        if (purchase.getId() == null) {
            purchase.setUserId(user.getId());
            purchase.setEmail(user.getEmail());
            purchase.setProduct(feature.product());
            purchase.setStatus(PurchaseStatus.PENDING);
            purchase.setCreatedAt(now);
        }
        // expected amount; FP-62 overwrites amount/currency with what Stripe actually charged
        purchase.setAmountCents(settings.fallbackAmountCents());
        purchase.setCurrency(settings.currency().toUpperCase(Locale.ROOT));
        purchase.setPriceId(priceId);
        purchase.setWithdrawalWaiverAccepted(true);
        purchase.setWithdrawalWaiverAcceptedAt(now);
        purchase.setWaiverTextVersion(version);
        purchase = purchases.saveAndFlush(purchase);

        String productName = productName(feature);
        CheckoutGateway.Session session = gateway.createSession(new CheckoutGateway.SessionRequest(
                user.getEmail(),
                String.valueOf(user.getId()),
                priceId,
                settings.fallbackAmountCents(),
                settings.currency(),
                productName,
                productName + ". " + confirmation,
                settings.successUrl(),
                settings.cancelUrl(),
                Map.of("purchase_id", String.valueOf(purchase.getId()), "product", feature.product())));

        // a reused row gets the new session id; a payment of its older session is still found via metadata.purchase_id
        purchase.setStripeSessionId(session.id());
        purchases.saveAndFlush(purchase);
        return new CheckoutResponse(session.url());
    }

    /** Line-item name (price_data) and description prefix. */
    static String productName(Feature feature) {
        return switch (feature) {
            case ROADMAP -> "Roadmap";
        };
    }
}
