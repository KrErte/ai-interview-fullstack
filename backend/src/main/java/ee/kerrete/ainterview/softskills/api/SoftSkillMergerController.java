package ee.kerrete.ainterview.softskills.api;

import ee.kerrete.ainterview.softskills.dto.SoftSkillMergeRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillMergeResponse;
import ee.kerrete.ainterview.softskills.service.SoftSkillMergeRestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/soft-skills")
@RequiredArgsConstructor
public class SoftSkillMergerController {

    private final SoftSkillMergeRestService mergeService;

    @PostMapping("/merge")
    public SoftSkillMergeResponse merge(@Valid @RequestBody SoftSkillMergeRequest request) {
        return mergeService.merge(request);
    }
}

