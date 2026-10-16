package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.billing.BillingSettings;
import ee.kerrete.ainterview.billing.EntitlementService;
import ee.kerrete.ainterview.billing.Purchase;
import ee.kerrete.ainterview.billing.PurchaseRepository;
import ee.kerrete.ainterview.billing.PurchaseStatus;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FP-61: checkout variant, see {@link CheckoutWebMvcTest}. */
@ActiveProfiles("test")
@WebMvcTest(controllers = CheckoutController.class, properties = {
        "billing.enabled=false",
        "billing.stripe-price-id=price_test_roadmap",
        "billing.frontend-base-url=https://app.example.test/",
        "billing.fallback-amount-cents=1900",
        "billing.currency=eur"})
@Import({SecurityConfig.class, BillingSettings.class, EntitlementService.class, CheckoutSettings.class,
        CheckoutService.class})
class CheckoutBillingOffWebMvcTest {

    static final String ME = "buyer@example.com";
    static final String OK_BODY = "{\"product\":\"roadmap\",\"withdrawalWaiverAccepted\":true,"
            + "\"withdrawalWaiverVersion\":\"waiver-v1-2026-10-16\"}";
    static final String WAIVER_TEXT = "Customer agreed to immediate delivery and waived the 14-day right of "
            + "withdrawal (waiver-v1-2026-10-16).";

    @Autowired
    MockMvc mvc;
    @MockBean
    JwtService jwtService;
    @MockBean
    AppUserRepository appUserRepository;
    @MockBean
    PurchaseRepository purchaseRepository;
    @MockBean
    CheckoutGateway gateway;

    final List<Purchase> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(appUserRepository.findByEmail(ME)).thenReturn(Optional.of(AppUser.builder().id(7L).email(ME)
                .password("x").role(UserRole.CANDIDATE).enabled(true).createdAt(LocalDateTime.now()).build()));
        when(purchaseRepository.saveAndFlush(any(Purchase.class))).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(42L);
            }
            saved.add(snapshot(p));
            return p;
        });
        when(gateway.createSession(any())).thenReturn(new CheckoutGateway.Session("cs_test_1", "https://checkout.stripe.test/c/cs_test_1"));
    }

    private static Purchase snapshot(Purchase p) {
        Purchase c = new Purchase();
        c.setId(p.getId());
        c.setUserId(p.getUserId());
        c.setEmail(p.getEmail());
        c.setProduct(p.getProduct());
        c.setStatus(p.getStatus());
        c.setStripeSessionId(p.getStripeSessionId());
        c.setAmountCents(p.getAmountCents());
        c.setCurrency(p.getCurrency());
        c.setPriceId(p.getPriceId());
        c.setWithdrawalWaiverAccepted(p.isWithdrawalWaiverAccepted());
        c.setWithdrawalWaiverAcceptedAt(p.getWithdrawalWaiverAcceptedAt());
        c.setWaiverTextVersion(p.getWaiverTextVersion());
        c.setCreatedAt(p.getCreatedAt());
        return c;
    }

    ResultActions checkout(String body) throws Exception {
        return mvc.perform(post("/api/billing/checkout").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    @WithMockUser(username = ME)
    void checkoutIs404WhenBillingIsOff() throws Exception {
        checkout(OK_BODY).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("billing_disabled"));
        verifyNoInteractions(gateway);
        verify(purchaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void stillNeedsLogin() throws Exception {
        checkout(OK_BODY).andExpect(status().isUnauthorized());
    }
}
