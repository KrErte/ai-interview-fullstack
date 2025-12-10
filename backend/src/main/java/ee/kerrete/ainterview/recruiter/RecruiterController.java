package ee.kerrete.ainterview.recruiter;

import ee.kerrete.ainterview.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
public class RecruiterController {

    private final RecruiterOverviewService recruiterOverviewService;

    @PostMapping("/overview")
    public RecruiterOverviewResponse getOverview(@RequestBody RecruiterOverviewRequest request,
                                                 Authentication authentication) {
        AppUser currentUser = resolveUser(authentication);
        return recruiterOverviewService.getOverview(currentUser, request);
    }

    private AppUser resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authenticated user required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUser appUser) {
            return appUser;
        }
        throw new IllegalArgumentException("Unexpected principal type");
    }
}

