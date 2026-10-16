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

/** FP-61: POST /api/billing/checkout through the real SecurityConfig, with the Stripe gateway mocked. */
@ActiveProfiles("test")
@WebMvcTest(controllers = CheckoutController.class, properties = {
        "billing.enabled=true",
        "billing.stripe-price-id=price_test_roadmap",
        "billing.frontend-base-url=https://app.example.test/",
        "billing.fallback-amount-cents=1900",
        "billing.currency=eur"})
@Import({SecurityConfig.class, BillingSettings.class, EntitlementService.class, CheckoutSettings.class,
        CheckoutService.class})
class CheckoutWebMvcTest {

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
    void needsLogin() throws Exception {
        checkout(OK_BODY).andExpect(status().isUnauthorized());
        verifyNoInteractions(gateway);
    }

    @Test
    @WithMockUser(username = ME)
    void happyPathCreatesPendingRowThenSession() throws Exception {
        checkout(OK_BODY).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.test/c/cs_test_1"));

        ArgumentCaptor<CheckoutGateway.SessionRequest> req = ArgumentCaptor.forClass(CheckoutGateway.SessionRequest.class);
        verify(gateway).createSession(req.capture());
        CheckoutGateway.SessionRequest r = req.getValue();
        assertThat(r.priceId()).isEqualTo("price_test_roadmap");
        assertThat(r.customerEmail()).isEqualTo(ME);
        assertThat(r.clientReferenceId()).isEqualTo("7");
        assertThat(r.description()).isEqualTo("Roadmap. " + WAIVER_TEXT);
        assertThat(r.successUrl()).isEqualTo("https://app.example.test/roadmap?paid=1");
        assertThat(r.cancelUrl()).isEqualTo("https://app.example.test/risk");
        assertThat(r.metadata()).containsEntry("purchase_id", "42").containsEntry("product", "roadmap").hasSize(2);

        // first save: pending row with the waiver, before the session exists; second: the session id
        assertThat(saved).hasSize(2);
        Purchase first = saved.get(0);
        assertThat(first.getStatus()).isEqualTo(PurchaseStatus.PENDING);
        assertThat(first.getStripeSessionId()).isNull();
        assertThat(first.getUserId()).isEqualTo(7L);
        assertThat(first.getProduct()).isEqualTo("roadmap");
        assertThat(first.isWithdrawalWaiverAccepted()).isTrue();
        assertThat(first.getWithdrawalWaiverAcceptedAt()).isNotNull();
        assertThat(first.getWaiverTextVersion()).isEqualTo("waiver-v1-2026-10-16");
        assertThat(first.getPriceId()).isEqualTo("price_test_roadmap");
        assertThat(first.getAmountCents()).isEqualTo(1900);
        assertThat(first.getCurrency()).isEqualTo("EUR");
        assertThat(saved.get(1).getStripeSessionId()).isEqualTo("cs_test_1");
        assertThat(saved.get(1).getStatus()).isEqualTo(PurchaseStatus.PENDING);
    }

    @Test
    @WithMockUser(username = ME)
    void reusesTheLatestPendingRow() throws Exception {
        Purchase old = new Purchase();
        old.setId(5L);
        old.setUserId(7L);
        old.setProduct("roadmap");
        old.setStatus(PurchaseStatus.PENDING);
        old.setStripeSessionId("cs_old");
        LocalDateTime created = LocalDateTime.of(2026, 10, 1, 12, 0);
        old.setCreatedAt(created);
        when(purchaseRepository.findFirstByUserIdAndProductAndStatusOrderByIdDesc(7L, "roadmap", PurchaseStatus.PENDING))
                .thenReturn(Optional.of(old));

        checkout(OK_BODY).andExpect(status().isOk());

        assertThat(saved).extracting(Purchase::getId).containsOnly(5L);
        assertThat(saved.get(0).getCreatedAt()).isEqualTo(created);
        assertThat(saved.get(1).getStripeSessionId()).isEqualTo("cs_test_1");
        ArgumentCaptor<CheckoutGateway.SessionRequest> req = ArgumentCaptor.forClass(CheckoutGateway.SessionRequest.class);
        verify(gateway).createSession(req.capture());
        assertThat(req.getValue().metadata()).containsEntry("purchase_id", "5");
    }

    @Test
    @WithMockUser(username = ME)
    void waiverMustBeTrue() throws Exception {
        for (String waiver : new String[]{"false", "null"}) {
            checkout("{\"product\":\"roadmap\",\"withdrawalWaiverAccepted\":" + waiver
                    + ",\"withdrawalWaiverVersion\":\"waiver-v1-2026-10-16\"}")
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("waiver_required"));
        }
        checkout("{\"product\":\"roadmap\",\"withdrawalWaiverVersion\":\"waiver-v1-2026-10-16\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("waiver_required"));
        verifyNoInteractions(gateway);
        verify(purchaseRepository, never()).saveAndFlush(any());
    }

    @Test
    @WithMockUser(username = ME)
    void waiverVersionMustBeCurrent() throws Exception {
        for (String version : new String[]{"\"waiver-v0\"", "\"\"", "null", "\"WAIVER-V1-2026-10-16\""}) {
            checkout("{\"product\":\"roadmap\",\"withdrawalWaiverAccepted\":true,\"withdrawalWaiverVersion\":" + version + "}")
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_waiver_version"));
        }
        verifyNoInteractions(gateway);
    }

    @Test
    @WithMockUser(username = ME)
    void unknownProductAndBadJsonAre400() throws Exception {
        checkout(OK_BODY.replace("roadmap", "team")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unknown_product"));
        checkout("{\"withdrawalWaiverAccepted\":true}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unknown_product"));
        checkout("{not json").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_request"));
        verifyNoInteractions(gateway);
    }

    @Test
    @WithMockUser(username = ME)
    void alreadyPaidIs409() throws Exception {
        when(purchaseRepository.existsByUserIdAndProductAndStatus(7L, "roadmap", PurchaseStatus.PAID)).thenReturn(true);
        checkout(OK_BODY).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("already_paid"));
        verifyNoInteractions(gateway);
        verify(purchaseRepository, never()).saveAndFlush(any());
    }

    @Test
    @WithMockUser(username = ME)
    void providerFailureIs503AndKeepsThePendingRow() throws Exception {
        when(gateway.createSession(any())).thenThrow(new CheckoutGatewayException("down", null));
        checkout(OK_BODY).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("payment_provider_unavailable"));
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStripeSessionId()).isNull();
    }

    @Test
    @WithMockUser(username = ME)
    void successRedirectUnlocksNothing() throws Exception {
        checkout(OK_BODY).andExpect(status().isOk());
        assertThat(saved).allSatisfy(p -> assertThat(p.getStatus()).isEqualTo(PurchaseStatus.PENDING));
        // the success url is a plain frontend page; no endpoint here marks anything paid
        assertThat(saved).noneMatch(p -> p.getStatus() == PurchaseStatus.PAID);
    }
}
