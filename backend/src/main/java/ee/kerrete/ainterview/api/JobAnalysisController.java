package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.security.AuthenticatedUser;
import ee.kerrete.ainterview.security.CurrentUser;
import ee.kerrete.ainterview.service.JobAnalysisService;
import ee.kerrete.ainterview.service.JobAnalysisStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Job Matcher REST controller.
 */
@RestController
@RequestMapping("/api/job-analysis")
@RequiredArgsConstructor
public class JobAnalysisController {

    private final JobAnalysisService jobAnalysisService;
    private final JobAnalysisStatsService jobAnalysisStatsService;

    @PostMapping
    public ResponseEntity<JobAnalysisResponse> analyze(
            @Valid @RequestBody JobAnalysisRequest request,
            @CurrentUser AuthenticatedUser user) {

        // Use authenticated user's email if not provided in request
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            if (user != null && user.email() != null) {
                request.setEmail(user.email());
            } else {
                throw new IllegalArgumentException("Email required");
            }
        }

        JobAnalysisResponse result = jobAnalysisService.analyze(request);

        // Log analysis statistics for profile
        jobAnalysisStatsService.recordAnalysis(request, result);

        return ResponseEntity.ok(result);
    }
}
