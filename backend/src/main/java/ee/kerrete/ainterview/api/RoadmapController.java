package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.RoadmapTaskDto;
import ee.kerrete.ainterview.dto.UpdateRoadmapTaskRequest;
import ee.kerrete.ainterview.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Roadmap of the authenticated user. The user is always taken from the auth principal.
 */
@RestController
@RequestMapping("/api/roadmap")
@CrossOrigin("*")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    /** Own roadmap. */
    @GetMapping
    public List<RoadmapTaskDto> getMyTasks() {
        return roadmapService.getTasksForEmail(CurrentUser.email());
    }

    /**
     * Legacy path kept for existing clients: only the user's own email is allowed (403 otherwise).
     * Prefer {@code GET /api/roadmap}.
     */
    @GetMapping("/{email}")
    public List<RoadmapTaskDto> getTasks(@PathVariable String email) {
        return roadmapService.getTasksForEmail(CurrentUser.requireSelf(email));
    }

    /** Updates one of the user's own tasks; any email in the body is ignored. */
    @PostMapping("/update")
    public List<RoadmapTaskDto> updateTask(@RequestBody UpdateRoadmapTaskRequest request) {
        request.setEmail(CurrentUser.email());
        return roadmapService.updateTask(request);
    }
}
