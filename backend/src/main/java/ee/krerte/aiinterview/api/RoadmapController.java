package ee.krerte.aiinterview.api;

import ee.krerte.aiinterview.model.RoadmapTask;
import ee.krerte.aiinterview.service.RoadmapService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmap")
@CrossOrigin("*")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @GetMapping
    public List<RoadmapTask> getTasks(@RequestParam String email) {
        return roadmapService.getTasksForEmail(email);
    }

    @PostMapping("/update")
    public List<RoadmapTask> updateTask(@RequestBody UpdateRequest dto) {
        return roadmapService.updateTask(dto.getEmail(), dto.getTaskKey(), dto.isCompleted());
    }

    @Data
    public static class UpdateRequest {
        private String email;
        private String taskKey;
        private boolean completed;
    }
}
