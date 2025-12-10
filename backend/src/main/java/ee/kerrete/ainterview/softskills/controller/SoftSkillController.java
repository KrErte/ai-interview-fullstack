package ee.kerrete.ainterview.softskills.controller;

import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationResponse;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileResponse;
import ee.kerrete.ainterview.softskills.service.SoftSkillEvaluationService;
import ee.kerrete.ainterview.softskills.service.SoftSkillMergerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<SoftSkillEvaluationResponse> createEvaluation(
            @RequestBody SoftSkillEvaluationRequest request
    ) {
        SoftSkillEvaluationResponse response = evaluationService.createEvaluation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetch all evaluations for a given user email.
     */
    @GetMapping("/evaluations")
    public ResponseEntity<List<SoftSkillEvaluationResponse>> getEvaluations(
            @RequestParam("email") String email
    ) {
        List<SoftSkillEvaluationResponse> responses = evaluationService.getEvaluationsForUser(email);
        return ResponseEntity.ok(responses);
    }

    /**
     * Trigger the merge algorithm for the given user, persist the merged profile and return it.
     */
    @PostMapping("/merge")
    public ResponseEntity<SoftSkillMergedProfileResponse> mergeForUser(
            @RequestParam("email") String email
    ) {
        return mergerService.mergeForUser(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Return the latest merged profile for a given user, if it exists.
     */
    @GetMapping("/profile")
    public ResponseEntity<SoftSkillMergedProfileResponse> getProfile(
            @RequestParam("email") String email
    ) {
        return mergerService.getLatestProfile(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}


