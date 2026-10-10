package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.ProfileOverviewResponse;
import ee.kerrete.ainterview.service.ProfileOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller profiili ülevaate jaoks.
 *
 * Frontend kutsub:
 *   GET /api/profile/overview (authenticated user)
 */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProfileOverviewController {

    private final ProfileOverviewService profileOverviewService;

    @GetMapping("/overview")
    public ResponseEntity<ProfileOverviewResponse> getProfileOverview() {
        ProfileOverviewResponse response = profileOverviewService.getOverview(CurrentUser.email());
        return ResponseEntity.ok(response);
    }
}
