package ee.kerrete.ainterview.team;

import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.team.TeamModels.CreateTeamRequest;
import ee.kerrete.ainterview.team.TeamModels.TeamInvite;
import ee.kerrete.ainterview.team.TeamModels.TeamReport;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Meeskonnaraport ilma kontota. Õigus tuleb raportitokenist (salajane link), mitte JWT-st;
 * kõik teed on SecurityConfig-is avalikud.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    static final Map<String, String> TOO_MANY = Map.of(
            "error", "rate_limited",
            "message", "Too many team reports were created. Try again in an hour.");

    private final TeamService service;
    /** Oma aken meeskondade loomiseks (sama sliding-window loogika kui LLM-i piiril, eraldi loendurid). */
    private final LlmRateLimiter createLimiter;

    public TeamController(
            TeamService service,
            @Value("${app.teams.create-per-client-per-hour:5}") int createPerClientPerHour,
            @Value("${app.teams.create-global-per-hour:30}") int createGlobalPerHour) {
        this.service = service;
        this.createLimiter = new LlmRateLimiter(createPerClientPerHour, createGlobalPerHour);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateTeamRequest request) {
        if (!createLimiter.tryAcquire(LlmRateLimiter.clientKey())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(TOO_MANY);
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.create(request));
    }

    @GetMapping("/join/{code}")
    public ResponseEntity<TeamInvite> join(@PathVariable String code) {
        return ResponseEntity.of(service.invite(code));
    }

    @GetMapping("/report/{token}")
    public ResponseEntity<TeamReport> report(@PathVariable String token) {
        return service.report(token)
                .map(r -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(r))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/report/{token}")
    public ResponseEntity<Void> delete(@PathVariable String token) {
        return service.delete(token) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
