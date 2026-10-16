package ee.kerrete.ainterview.team;

import ee.kerrete.ainterview.team.TeamModels.CreateTeamRequest;
import ee.kerrete.ainterview.team.TeamModels.CreatedTeam;
import ee.kerrete.ainterview.team.TeamModels.TeamInvite;
import ee.kerrete.ainterview.team.TeamModels.TeamReport;
import jakarta.validation.Valid;
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

/**
 * Meeskonnaraport ilma kontota. Õigus tuleb raportitokenist (salajane link), mitte JWT-st;
 * kõik teed on SecurityConfig-is avalikud.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreatedTeam> create(@Valid @RequestBody CreateTeamRequest request) {
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
