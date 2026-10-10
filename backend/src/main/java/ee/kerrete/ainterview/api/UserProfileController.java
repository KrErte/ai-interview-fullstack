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

    // The user is always the authenticated principal (CurrentUser); an ?email= parameter sent by
    // older clients is ignored.

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me() {
        return ResponseEntity.ok(userProfileService.getProfile(CurrentUser.email()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> save(@RequestBody UserProfileDto dto) {
        return ResponseEntity.ok(userProfileService.saveProfile(CurrentUser.email(), dto));
    }
}
