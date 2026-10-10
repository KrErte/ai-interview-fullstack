package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import ee.kerrete.ainterview.dto.UserProfileDto;
import ee.kerrete.ainterview.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(@RequestParam(value = "email", required = false) String email) {
        String resolved = resolveEmail(email);
        return ResponseEntity.ok(userProfileService.getProfile(resolved));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> save(@RequestParam(value = "email", required = false) String email,
                                               @RequestBody UserProfileDto dto) {
        String resolved = resolveEmail(email);
        return ResponseEntity.ok(userProfileService.saveProfile(resolved, dto));
    }

    /** FP-17: always the authenticated user; an ?email= parameter is ignored. */
    private String resolveEmail(String ignored) {
        return CurrentUser.email();
    }
}








