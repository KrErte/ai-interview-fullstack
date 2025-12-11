package ee.kerrete.ainterview.training.api;

import ee.kerrete.ainterview.security.AuthenticatedUser;
import ee.kerrete.ainterview.security.CurrentUser;
import ee.kerrete.ainterview.training.dto.TrainingStatusDto;
import ee.kerrete.ainterview.training.dto.TrainingTaskCompletionRequest;
import ee.kerrete.ainterview.training.service.TrainingTaskStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingTaskStatusController {

    private final TrainingTaskStatusService service;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public TrainingStatusDto getStatus(@CurrentUser AuthenticatedUser user) {
        String email = resolveEmail(user);
        return service.getStatusForUser(email);
    }

    @PostMapping("/complete/{taskKey}")
    @PreAuthorize("isAuthenticated()")
    public TrainingStatusDto setCompletion(
        @PathVariable String taskKey,
        @Valid @RequestBody TrainingTaskCompletionRequest request,
        @CurrentUser AuthenticatedUser user
    ) {
        String email = resolveEmail(user);
        boolean completed = Boolean.TRUE.equals(request.getCompleted());
        return service.setTaskCompletion(email, taskKey, completed);
    }

    private String resolveEmail(AuthenticatedUser user) {
        if (user == null || !StringUtils.hasText(user.email())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user.email();
    }
}

