package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.AdaptiveAnalysisRequest;
import ee.kerrete.ainterview.dto.AdaptiveAnalysisResponse;
import ee.kerrete.ainterview.dto.TrainingTaskRequest;
import ee.kerrete.ainterview.service.AiAdaptiveService;
import ee.kerrete.ainterview.service.TrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API kiht AI skill coach'i jaoks.
 *
 * /api/skill-coach/analyze  – ainult AI-analüüs, ei salvesta DB-sse
 * /api/skill-coach/answer   – analüüs + vastuse salvestamine treeninguna
 */
@RestController
@RequestMapping("/api/skill-coach")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SkillCoachController {

    private final AiAdaptiveService aiAdaptiveService;
    private final TrainingService trainingService;

    /**
     * Lihtne endpoint: võtab küsimuse + vastuse ja tagastab AI analüüsi.
     * DB-sse midagi ei salvestata.
     */
    @PostMapping("/analyze")
    public ResponseEntity<AdaptiveAnalysisResponse> analyze(
            @RequestBody AdaptiveAnalysisRequest request
    ) {
        AdaptiveAnalysisResponse response = aiAdaptiveService.analyzeAnswer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint, mis:
     *  1) laseb AI-l vastust analüüsida
     *  2) salvestab vastuse treeningtaskina
     *  3) tagastab sama AI analüüsi tagasi fronti
     */
    @PostMapping("/answer")
    public ResponseEntity<AdaptiveAnalysisResponse> answerAndSave(
            @RequestBody AdaptiveAnalysisRequest request
    ) {
        // 1. Küsi AI-lt analüüs
        AdaptiveAnalysisResponse response = aiAdaptiveService.analyzeAnswer(request);

        // 2. Otsusta, mis taskKey alla see treening läheb.
        // MVP: kasutame roadmapItemId, kui see on olemas, muidu genereerime hash'i küsimuse põhjal.
        String taskKey;
        if (request.getRoadmapItemId() != null && !request.getRoadmapItemId().isBlank()) {
            taskKey = request.getRoadmapItemId();
        } else {
            taskKey = "coach_" + Math.abs(
                    (request.getQuestion() != null ? request.getQuestion() : "").hashCode()
            );
        }

        // 3. Ehita TrainingTaskRequest ja salvesta treeningusse
        TrainingTaskRequest taskRequest = TrainingTaskRequest.builder()
                .email(request.getEmail())
                .taskKey(taskKey)
                .answerText(request.getAnswer())
                .completed(true)
                // kui kunagi lisad numbrilise skoori AdaptiveAnalysisResponse sisse,
                // saad selle siia .score(...) panna
                .build();

        trainingService.updateTaskStatus(taskRequest);

        log.info("Skill-coach answer stored for email={} taskKey={}", request.getEmail(), taskKey);

        // 4. Tagasta AI analüüs fronti
        return ResponseEntity.ok(response);
    }
}
