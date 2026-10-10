package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.JobMatchDto;
import ee.kerrete.ainterview.dto.JobMatchRequest;
import ee.kerrete.ainterview.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-match")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;

    @PostMapping
    public ResponseEntity<List<JobMatchDto>> match(@RequestBody JobMatchRequest request) {
        request.setEmail(CurrentUser.email());
        return ResponseEntity.ok(jobMatchService.match(request));
    }

    @GetMapping
    public ResponseEntity<List<JobMatchDto>> list(@RequestParam(value = "targetRole", required = false) String targetRole) {
        JobMatchRequest req = new JobMatchRequest();
        req.setEmail(CurrentUser.email());
        req.setTargetRole(targetRole);
        return ResponseEntity.ok(jobMatchService.match(req));
    }

}








