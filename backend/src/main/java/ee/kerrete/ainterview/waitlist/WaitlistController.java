package ee.kerrete.ainterview.waitlist;

import ee.kerrete.ainterview.ai.LlmRateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Ootelist toodetele, mida veel ei müüda (võltsuks, docs/vajadus-ai-ajastul-2026-10-17.md). Avalik, ilma kontota.
 * Sama e-post sama toote kohta salvestatakse üks kord; korduv liitumine vastab samuti 204, et vastus ei
 * paljastaks, kas aadress on juba nimekirjas.
 */
@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    static final Map<String, String> TOO_MANY = Map.of(
            "error", "rate_limited",
            "message", "Too many sign-ups from here. Try again in an hour.");

    public record JoinRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "interview-prep") String product,
            @Size(max = 128) @Pattern(regexp = "[a-z0-9-]*") String professionId,
            @Size(max = 32) @Pattern(regexp = "[a-z-]*") String source
    ) {
    }

    private final WaitlistRepository signups;
    /** Oma aken ootelistile (sama sliding-window loogika kui LLM-i piiril, eraldi loendurid). */
    private final LlmRateLimiter limiter;

    public WaitlistController(
            WaitlistRepository signups,
            @Value("${app.waitlist.per-client-per-hour:10}") int perClientPerHour,
            @Value("${app.waitlist.global-per-hour:300}") int globalPerHour) {
        this.signups = signups;
        this.limiter = new LlmRateLimiter(perClientPerHour, globalPerHour);
    }

    @PostMapping
    public ResponseEntity<?> join(@Valid @RequestBody JoinRequest request) {
        if (!limiter.tryAcquire(LlmRateLimiter.clientKey())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(TOO_MANY);
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!signups.existsByEmailAndProduct(email, request.product())) {
            WaitlistSignup s = new WaitlistSignup();
            s.setEmail(email);
            s.setProduct(request.product());
            s.setProfessionId(blankToNull(request.professionId()));
            s.setSource(blankToNull(request.source()));
            s.setCreatedAt(LocalDateTime.now());
            try {
                signups.saveAndFlush(s);
            } catch (DataIntegrityViolationException e) {
                // two requests at once: the unique (email, product) key already holds the address
            }
        }
        return ResponseEntity.noContent().build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
