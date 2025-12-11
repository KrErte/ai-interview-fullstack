package ee.kerrete.ainterview.softskills.controller;

import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationResponse;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileResponse;
import ee.kerrete.ainterview.softskills.service.SoftSkillEvaluationService;
import ee.kerrete.ainterview.softskills.service.SoftSkillMergerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/soft-skills")
@RequiredArgsConstructor
public class SoftSkillController {

    private final SoftSkillEvaluationService evaluationService;
    private final SoftSkillMergerService mergerService;

    /**
     * Store a single evaluation from HR/TECH_LEAD/TEAM_LEAD or other sources.
     */
    @PostMapping("/evaluations")
    public ResponseEntity<List<SoftSkillEvaluationResponse>> createEvaluation(
            @RequestBody SoftSkillEvaluationRequest request
    ) {
        List<SoftSkillEvaluationResponse> responses = evaluationService.createEvaluations(request);
        return ResponseEntity.ok(responses);
    }

    /**
     * Fetch all evaluations for a given user email.
     *
     * Note: email is optional at the request mapping level to avoid
     * {@link org.springframework.web.bind.MissingServletRequestParameterException},
     * but we still validate it manually to return a clear 400 instead of a 500.
     */
    @GetMapping("/evaluations")
    public ResponseEntity<List<SoftSkillEvaluationResponse>> getEvaluations(
            @RequestParam(value = "email", required = false) String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter 'email' is required");
        }
        List<SoftSkillEvaluationResponse> responses = evaluationService.getEvaluationsForUser(email);
        return ResponseEntity.ok(responses);
    }

    /**
     * Return the latest merged profile for a given user, if it exists.
     *
     * Similar to {@link #getEvaluations(String)}, email is optional in the
     * mapping but validated manually for clearer error handling.
     */
    @GetMapping("/profile")
    public ResponseEntity<SoftSkillMergedProfileResponse> getProfile(
            @RequestParam(value = "email", required = false) String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter 'email' is required");
        }
        return mergerService.getLatestProfile(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}


