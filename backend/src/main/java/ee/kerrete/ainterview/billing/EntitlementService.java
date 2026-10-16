package ee.kerrete.ainterview.billing;

import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The single place that decides whether a paid feature is locked for a principal (FP-60).
 * Controllers and views never read billing settings themselves; they call {@link #isLocked(Feature, String)}.
 */
@Service
public class EntitlementService {

    /** purchase.product of the roadmap; kept for callers that need the code. */
    public static final String ROADMAP = Feature.ROADMAP.product();

    private final BillingSettings settings;
    private final AppUserRepository users;
    private final PurchaseRepository purchases;

    public EntitlementService(BillingSettings settings, AppUserRepository users, PurchaseRepository purchases) {
        this.settings = settings;
        this.users = users;
        this.purchases = purchases;
    }

    /**
     * @param email the authenticated principal's exact email ({@code CurrentUser.email()}), or null for an
     *              anonymous caller (public /assess, public job pages)
     * @return false while billing.enabled=false; otherwise true unless the user is dev-entitled (local/dev only)
     * or has a purchase with status paid for the feature's product (pending/refunded do not count)
     */
    @Transactional(readOnly = true)
    public boolean isLocked(Feature feature, String email) {
        if (!settings.enabled()) {
            return false;
        }
        if (email == null || email.isBlank()) {
            return true;
        }
        Optional<AppUser> user = users.findByEmail(email);
        if (user.isEmpty()) {
            return true;
        }
        String role = user.get().getRole() == null ? null : user.get().getRole().name();
        if (settings.devEntitled(email, role)) {
            return false;
        }
        return !purchases.existsByUserIdAndProductAndStatus(user.get().getId(), feature.product(), PurchaseStatus.PAID);
    }

    /**
     * Public /assess: while billing is on, the roadmap is always cut from it (for everyone, paid or not); the full
     * roadmap only comes from POST /api/ai-readiness/roadmap via {@link #require(Feature, String)}.
     */
    public boolean lockPublicAssessment() {
        return settings.enabled();
    }

    public boolean billingEnabled() {
        return settings.enabled();
    }

    public boolean hasRoadmap(String email) {
        return !isLocked(Feature.ROADMAP, email);
    }

    /** @throws PaymentRequiredException when the feature is locked for this principal */
    public void require(Feature feature, String email) {
        if (isLocked(feature, email)) {
            throw new PaymentRequiredException();
        }
    }
}
