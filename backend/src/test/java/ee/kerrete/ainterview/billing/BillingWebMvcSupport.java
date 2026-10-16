package ee.kerrete.ainterview.billing;

import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;

import java.time.LocalDateTime;

final class BillingWebMvcSupport {

    static final String ME = "buyer@example.com";
    static final String ROADMAP_BODY = "{\"professionId\":\"translator\",\"answers\":{}}";

    private BillingWebMvcSupport() {
    }

    static AppUser user(long id, String email, UserRole role) {
        return AppUser.builder().id(id).email(email).password("x").role(role).enabled(true)
                .createdAt(LocalDateTime.now()).build();
    }
}
