package ee.krerte.aiinterview.controller;

import ee.krerte.aiinterview.dto.WorkstyleAnswerRequest;
import ee.krerte.aiinterview.model.WorkstyleSession;
import ee.krerte.aiinterview.service.WorkstyleSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workstyle")
public class WorkstyleSessionController {
    @Autowired
    private WorkstyleSessionService service;

    @PostMapping("/start")
    public ResponseEntity<WorkstyleSession> start(@RequestParam String email) {
        return ResponseEntity.ok(service.startSession(email));
    }

    @PostMapping("/answer")
    public ResponseEntity<WorkstyleSession> answer(@RequestBody WorkstyleAnswerRequest req) {
        return ResponseEntity.ok(service.answer(req.getSessionId(), req.getAnswer()));
    }

    @GetMapping("/session/{id}")
    public ResponseEntity<WorkstyleSession> getSession(@PathVariable UUID id) {
        return service.getSession(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
