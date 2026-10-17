package ee.kerrete.ainterview.billing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class BillingExceptionHandler {

    @ExceptionHandler(PaymentRequiredException.class)
    public ResponseEntity<Map<String, String>> paymentRequired(PaymentRequiredException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("error", "payment_required"));
    }
}
