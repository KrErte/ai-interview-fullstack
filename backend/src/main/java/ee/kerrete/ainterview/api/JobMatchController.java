package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.JobMatchDto;
import ee.kerrete.ainterview.dto.JobMatchRequest;
import ee.kerrete.ainterview.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            request.setEmail(resolveEmail());
        }
        return ResponseEntity.ok(jobMatchService.match(request));
    }

    @GetMapping
    public ResponseEntity<List<JobMatchDto>> list(@RequestParam(value = "email", required = false) String email,
                                                  @RequestParam(value = "targetRole", required = false) String targetRole) {
        JobMatchRequest req = new JobMatchRequest();
        req.setEmail(email != null && !email.isBlank() ? email : resolveEmail());
        req.setTargetRole(targetRole);
        return ResponseEntity.ok(jobMatchService.match(req));
    }

    private String resolveEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String principal) {
            return principal;
        }
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new IllegalArgumentException("Email is required");
    }
}








