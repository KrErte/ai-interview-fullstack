package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.CoachAnswerRequest;
import ee.kerrete.ainterview.dto.CoachAnswerResponse;
import ee.kerrete.ainterview.dto.CoachStateResponse;
import ee.kerrete.ainterview.service.SoftSkillsCoachService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SoftSkillsCoachController {

    private final SoftSkillsCoachService softSkillsCoachService;

    @GetMapping("/state")
    public ResponseEntity<?> getState() {
        String email = currentEmail();
        // FP-20: OpenAiCoachException (an AiUnavailableException) -> 503 {"error":"ai_unavailable"}
        CoachStateResponse response = softSkillsCoachService.getState(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/answer")
    public ResponseEntity<?> answer(@RequestBody CoachAnswerRequest request) {
        String email = currentEmail();
        // FP-20: OpenAiCoachException (an AiUnavailableException) -> 503 {"error":"ai_unavailable"}
        CoachAnswerResponse response = softSkillsCoachService.submitAnswer(email, request);
        return ResponseEntity.ok(response);
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kasutaja pole autentitud");
        }
        return auth.getName();
    }
}

