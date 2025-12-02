package ee.krerte.aiinterview.controller;

import ee.krerte.aiinterview.dto.AdaptiveAnalysisRequest;
import ee.krerte.aiinterview.dto.AdaptiveAnalysisResponse;
import ee.krerte.aiinterview.dto.SoftSkillQuestionRequest;
import ee.krerte.aiinterview.dto.SoftSkillQuestionResponse;
import ee.krerte.aiinterview.service.AiAdaptiveService;
import ee.krerte.aiinterview.service.SoftSkillQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trainer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdaptiveTrainerController {

    private final AiAdaptiveService aiAdaptiveService;
    private final SoftSkillQuestionService softSkillQuestionService;

    /**
     * Vastuse analüüs (olemasolev).
     */
    @PostMapping("/analyze")
    public ResponseEntity<AdaptiveAnalysisResponse> analyze(
            @RequestBody AdaptiveAnalysisRequest request
    ) {
        return ResponseEntity.ok(aiAdaptiveService.analyzeAnswer(request));
    }

    /**
     * Soft-skilli treeningküsija:
     *  - kui previousQuestion/previousAnswer täidetud → salvestab need DB-sse + analüüsib AI-ga
     *  - seejärel küsib OpenAI-lt järgmise küsimuse sama roadmapi teema kohta
     */
    @PostMapping("/soft-question")
    public ResponseEntity<SoftSkillQuestionResponse> nextSoftQuestion(
            @RequestBody SoftSkillQuestionRequest request
    ) {
        SoftSkillQuestionResponse response = softSkillQuestionService.generateQuestion(request);
        return ResponseEntity.ok(response);
    }
}
