package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.SkillMatrixResponse;
import ee.kerrete.ainterview.service.SkillMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skills")  // <-- MUUTSIME
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SkillMatrixController {

    private final SkillMatrixService skillMatrixService;

    @GetMapping("/matrix")     // <-- /api/skills/matrix
    public ResponseEntity<SkillMatrixResponse> getSkillMatrix(
            @RequestParam String email) {

        SkillMatrixResponse response = skillMatrixService.getSkillMatrix(email);
        return ResponseEntity.ok(response);
    }
}
