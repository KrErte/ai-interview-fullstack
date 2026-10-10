package ee.kerrete.ainterview.api.controller;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.model.ProgressSummary;
import ee.kerrete.ainterview.service.SessionService;
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
     * GET /api/candidate/progress (authenticated user)
     */
    @GetMapping("/progress")
    public ResponseEntity<ProgressSummary> getProgress() {
        ProgressSummary summary = sessionService.getProgressSummary(CurrentUser.email());
        return ResponseEntity.ok(summary);
    }
}
