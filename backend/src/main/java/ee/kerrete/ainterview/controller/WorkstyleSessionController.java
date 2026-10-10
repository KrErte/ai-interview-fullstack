package ee.kerrete.ainterview.controller;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.WorkstyleAnswerRequest;
import ee.kerrete.ainterview.model.WorkstyleSession;
import ee.kerrete.ainterview.service.WorkstyleSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workstyle")
public class WorkstyleSessionController {
    @Autowired
    private WorkstyleSessionService service;

    @PostMapping("/start")
    public ResponseEntity<WorkstyleSession> start() {
        // FP-17: always the authenticated user; an ?email= parameter is ignored
        return ResponseEntity.ok(service.startSession(CurrentUser.email()));
    }

    @PostMapping("/answer")
    public ResponseEntity<WorkstyleSession> answer(@RequestBody WorkstyleAnswerRequest req) {
        // only the owner may answer: 404 if the session does not exist, 403 if it is someone else's
        WorkstyleSession session = service.getSession(req.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        CurrentUser.requireSelf(session.getEmail());
        return ResponseEntity.ok(service.answer(req.getSessionId(), req.getAnswer()));
    }

    @GetMapping("/session/{id}")
    public ResponseEntity<WorkstyleSession> getSession(@PathVariable UUID id) {
        return service.getSession(id)
            .map(session -> {
                CurrentUser.requireSelf(session.getEmail()); // 403 for another user's session
                return ResponseEntity.ok(session);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
