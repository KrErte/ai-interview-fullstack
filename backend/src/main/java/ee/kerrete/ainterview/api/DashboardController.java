package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.DashboardResponse;
import ee.kerrete.ainterview.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> get(@RequestParam(value = "email", required = false) String email) {
        String resolved = email != null && !email.isBlank() ? email : resolveEmail();
        return ResponseEntity.ok(dashboardService.get(resolved));
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








