package ee.krerte.aiinterview.api;

import ee.krerte.aiinterview.dto.CreateInterviewSessionRequest;
import ee.krerte.aiinterview.dto.CreateInterviewSessionResponse;
import ee.krerte.aiinterview.service.InterviewSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview-sessions")
@CrossOrigin(origins = "http://localhost:4200")
public class InterviewSessionController {

    private final InterviewSessionService sessionService;

    public InterviewSessionController(InterviewSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<CreateInterviewSessionResponse> createSession(
            @RequestBody CreateInterviewSessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.createSession(request));
    }
}
