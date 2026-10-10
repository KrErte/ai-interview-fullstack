package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.SoftSkillMatrixResponse;
import ee.kerrete.ainterview.service.SoftSkillMatrixService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Soft-skill matrixi API.
 *
 * GET /api/soft-skills (authenticated user)
 */
@RestController
@RequestMapping("/api/soft-skills")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SoftSkillMatrixController {

    private final SoftSkillMatrixService softSkillMatrixService;

    @GetMapping
    public ResponseEntity<SoftSkillMatrixResponse> getSoftSkills() {
        String email = CurrentUser.email();
        log.info("Soft skill matrix requested for email='{}'", email);
        SoftSkillMatrixResponse response = softSkillMatrixService.calculateFor(email);
        return ResponseEntity.ok(response);
    }
}
