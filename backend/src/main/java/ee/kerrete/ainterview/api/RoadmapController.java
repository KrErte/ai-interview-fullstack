package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmap")
@CrossOrigin("*")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @GetMapping("/{email}")
    public List<RoadmapTaskDto> getTasks(@PathVariable String email) {
        return roadmapService.getTasksForEmail(email);
    }

    @PostMapping("/update")
    public List<RoadmapTaskDto> updateTask(@RequestBody UpdateRoadmapTaskRequest request) {
        return roadmapService.updateTask(request);
    }
}
