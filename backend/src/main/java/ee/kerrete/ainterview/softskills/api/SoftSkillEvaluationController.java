package ee.kerrete.ainterview.softskills.api;

import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationResponse;
import ee.kerrete.ainterview.softskills.service.SoftSkillEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/soft-skill/evaluations")
@RequiredArgsConstructor
@Slf4j
public class SoftSkillEvaluationController {

    private final SoftSkillEvaluationService evaluationService;

    /**
     * Accepted JSON contract (v0.1):
     * {
     *   "email": "candidate@example.com",
     *   "sourceType": "HR",
     *   "scores": [
     *     { "dimensionKey": "adaptability", "score": 3, "explanation": "note" }
     *   ]
     * }
     */
    @PostMapping
    public ResponseEntity<List<SoftSkillEvaluationResponse>> createEvaluation(
        @Valid @RequestBody SoftSkillEvaluationRequest request
    ) {
        List<SoftSkillEvaluationResponse> responses = evaluationService.createEvaluations(request);
        log.info("Soft skill evaluation saved for candidate {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}

