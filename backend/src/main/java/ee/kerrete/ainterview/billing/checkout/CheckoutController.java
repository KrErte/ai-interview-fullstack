package ee.kerrete.ainterview.billing.checkout;

import ee.kerrete.ainterview.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** POST /api/billing/checkout (login required: not in SecurityConfig's public list). */
@RestController
@RequestMapping("/api/billing")
public class CheckoutController {

    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping("/checkout")
    public CheckoutService.CheckoutResponse checkout(@RequestBody CheckoutService.CheckoutRequest request) {
        return checkout.checkout(CurrentUser.email(), request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> rejected(CheckoutException e) {
        return ResponseEntity.status(e.status()).body(Map.of("error", e.code()));
    }

    @ExceptionHandler(CheckoutGatewayException.class)
    public ResponseEntity<Map<String, String>> providerDown(CheckoutGatewayException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "payment_provider_unavailable"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "invalid_request"));
    }
}
