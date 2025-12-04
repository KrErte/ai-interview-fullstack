package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.TrainingProgressResponse;
import ee.kerrete.ainterview.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProgressController {

    private final TrainingService trainingService;

    /**
     * Profiili progressiriba: GET /api/progress?email=...
     */
    @GetMapping("/progress")
    public TrainingProgressResponse getProgress(@RequestParam String email) {
        return trainingService.getProgress(email);
    }
}
