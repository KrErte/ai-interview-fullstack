package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.SkillPlanRequest;
import ee.kerrete.ainterview.dto.SkillPlanResponse;
import ee.kerrete.ainterview.service.SkillPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skill-plan")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
@RequiredArgsConstructor
public class SkillPlanController {

    private final SkillPlanService skillPlanService;

    @PostMapping
    public ResponseEntity<SkillPlanResponse> buildPlan(@RequestBody SkillPlanRequest request) {
        SkillPlanResponse response = skillPlanService.buildPlan(request);
        return ResponseEntity.ok(response);
    }
}
