package ee.kerrete.ainterview.billing;

import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Who may use paid products (FP-60). Only a purchase with status paid counts. */
@Service
public class EntitlementService {

    public static final String ROADMAP = "roadmap";

    private final BillingSettings settings;
    private final AppUserRepository users;
    private final PurchaseRepository purchases;

    public EntitlementService(BillingSettings settings, AppUserRepository users, PurchaseRepository purchases) {
        this.settings = settings;
        this.users = users;
        this.purchases = purchases;
    }

    /**
     * @param email the authenticated principal's email ({@code CurrentUser.email()}), looked up exactly
     * @return true when billing is disabled, the user is dev-entitled (local/dev only) or has a paid roadmap purchase
     */
    @Transactional(readOnly = true)
    public boolean hasRoadmap(String email) {
        if (!settings.enabled()) {
            return true;
        }
        Optional<AppUser> user = users.findByEmail(email);
        if (user.isEmpty()) {
            return false;
        }
        String role = user.get().getRole() == null ? null : user.get().getRole().name();
        if (settings.devEntitled(email, role)) {
            return true;
        }
        return purchases.existsByUserIdAndProductAndStatus(user.get().getId(), ROADMAP, PurchaseStatus.PAID);
    }

    /** @throws PaymentRequiredException when {@link #hasRoadmap(String)} is false */
    public void requireRoadmap(String email) {
        if (!hasRoadmap(email)) {
            throw new PaymentRequiredException();
        }
    }
}
