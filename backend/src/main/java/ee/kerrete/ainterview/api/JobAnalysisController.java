package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.service.JobAnalysisService;
import ee.kerrete.ainterview.service.JobAnalysisStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        JobAnalysisResponse result = jobAnalysisService.analyze(request);

        // logime analüüsi statistika teenusesse (profiili jaoks)
        jobAnalysisStatsService.recordAnalysis(request, result);

        return ResponseEntity.ok(result);
    }
}
