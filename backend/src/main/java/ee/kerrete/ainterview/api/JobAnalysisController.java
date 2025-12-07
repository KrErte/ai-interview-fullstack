package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.service.JobAnalysisService;
import ee.kerrete.ainterview.service.JobAnalysisStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Job Matcheri REST controller.
 */
@RestController
@RequestMapping("/api/job-analysis")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JobAnalysisController {

    private final JobAnalysisService jobAnalysisService;
    private final JobAnalysisStatsService jobAnalysisStatsService;

    @PostMapping
    public ResponseEntity<JobAnalysisResponse> analyze(@RequestBody JobAnalysisRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            request.setEmail(resolveEmail());
        }
        JobAnalysisResponse result = jobAnalysisService.analyze(request);

        // logime analüüsi statistika teenusesse (profiili jaoks)
        jobAnalysisStatsService.recordAnalysis(request, result);

        return ResponseEntity.ok(result);
    }

    private String resolveEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String principal) {
            return principal;
        }
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new IllegalArgumentException("Email required");
    }
}
