package ee.krerte.aiinterview.api.controller;

import ee.krerte.aiinterview.model.ProgressSummary;
import ee.krerte.aiinterview.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidateProfileController {

    private final SessionService sessionService;

    /**
     * GET /api/candidate/progress?email=someone@example.com
     */
    @GetMapping("/progress")
    public ResponseEntity<ProgressSummary> getProgress(@RequestParam String email) {
        ProgressSummary summary = sessionService.getProgressSummary(email);
        return ResponseEntity.ok(summary);
    }
}
