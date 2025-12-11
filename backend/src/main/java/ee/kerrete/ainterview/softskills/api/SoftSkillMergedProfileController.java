package ee.kerrete.ainterview.softskills.api;

import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileView;
import ee.kerrete.ainterview.softskills.service.SoftSkillMergedProfileViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/soft-skill")
@RequiredArgsConstructor
public class SoftSkillMergedProfileController {

    private final SoftSkillMergedProfileViewService mergedProfileViewService;

    @GetMapping("/merged-profile")
    public SoftSkillMergedProfileView getMergedProfile(
        @RequestParam(value = "email", required = false) String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query parameter 'email' is required");
        }
        return mergedProfileViewService.getMergedProfile(email.trim());
    }
}


