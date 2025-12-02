package ee.krerte.aiinterview.api;

import ee.krerte.aiinterview.dto.ProfileOverviewResponse;
import ee.krerte.aiinterview.service.ProfileOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller profiili ülevaate jaoks.
 *
 * Frontend kutsub:
 *   GET /api/profile/overview?email=kasutaja@email.com
 */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProfileOverviewController {

    private final ProfileOverviewService profileOverviewService;

    @GetMapping("/overview")
    public ResponseEntity<ProfileOverviewResponse> getProfileOverview(
            @RequestParam(value = "email", required = false) String email
    ) {
        ProfileOverviewResponse response = profileOverviewService.getOverview(email);
        return ResponseEntity.ok(response);
    }
}
