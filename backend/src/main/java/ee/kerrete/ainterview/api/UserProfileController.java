package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.dto.UserProfileDto;
import ee.kerrete.ainterview.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private String resolveEmail(String fallback) {
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String principal) {
            return principal;
        }
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new IllegalArgumentException("Unable to resolve user email");
    }
}












