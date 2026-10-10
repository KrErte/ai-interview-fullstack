package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.MindsetRoadmapDetail;
import ee.kerrete.ainterview.dto.MindsetRoadmapSummary;
import ee.kerrete.ainterview.dto.TrainingTaskRequest;
import ee.kerrete.ainterview.service.MindsetRoadmapService;
import ee.kerrete.ainterview.service.TrainingService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mindset-roadmapi API.
 *
 * - GET  /api/mindset/roadmaps            (authenticated user)
 * - GET  /api/mindset/roadmaps/{roadmapKey} (authenticated user)
 * - POST /api/mindset/task  (uuendab ühe taski ja tagastab vastava roadmapi detailid)
 */
@RestController
@RequestMapping("/api/mindset")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MindsetRoadmapController {

    private final MindsetRoadmapService mindsetRoadmapService;
    private final TrainingService trainingService;
    private final Validator validator;

    @GetMapping("/roadmaps")
    public ResponseEntity<List<MindsetRoadmapSummary>> getRoadmaps() {
        // FP-17: always the authenticated user; an ?email= parameter is ignored
        List<MindsetRoadmapSummary> roadmaps = mindsetRoadmapService.getRoadmapsForEmail(CurrentUser.email());
        return ResponseEntity.ok(roadmaps);
    }

    @GetMapping("/roadmaps/{roadmapKey}")
    public ResponseEntity<MindsetRoadmapDetail> getRoadmapDetail(
            @PathVariable String roadmapKey
    ) {
        MindsetRoadmapDetail detail = mindsetRoadmapService.getRoadmapDetail(CurrentUser.email(), roadmapKey);
        return ResponseEntity.ok(detail);
    }

    /**
     * Uuendab ühe mindset-taski (nt checkbox "done") ja tagastab sama roadmapi detailivaate.
     */
    @PostMapping("/task")
    public ResponseEntity<MindsetRoadmapDetail> updateTaskAndReturnRoadmap(
            @RequestBody TrainingTaskRequest request
    ) {
        RequestValidation.withPrincipalEmail(validator, request, TrainingTaskRequest::setEmail); // email is @NotBlank on the DTO
        // 1. Uuendame treening-taski ja koondprogressi (kasutame olemasolevat loogikat)
        trainingService.updateTaskStatus(request);

        // 2. Leiame, millisesse roadmapi see task kuulub ja tagastame detailid
        String roadmapKey = mindsetRoadmapService.resolveRoadmapKeyFromTaskKey(request.resolveTaskKey());
        MindsetRoadmapDetail detail =
                mindsetRoadmapService.getRoadmapDetail(request.getEmail(), roadmapKey);

        return ResponseEntity.ok(detail);
    }
}
