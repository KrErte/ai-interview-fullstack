package ee.kerrete.ainterview.billing;

import ee.kerrete.ainterview.auth.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    public record EntitlementResponse(boolean roadmap) {
    }

    private final EntitlementService entitlements;

    public BillingController(EntitlementService entitlements) {
        this.entitlements = entitlements;
    }

    /** Auth required (not in SecurityConfig's public list). Always true while billing.enabled=false. */
    @GetMapping("/entitlement")
    public EntitlementResponse entitlement() {
        return new EntitlementResponse(entitlements.hasRoadmap(CurrentUser.email()));
    }
}
