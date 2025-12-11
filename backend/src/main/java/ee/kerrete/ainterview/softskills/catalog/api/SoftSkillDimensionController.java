package ee.kerrete.ainterview.softskills.catalog.api;

import ee.kerrete.ainterview.softskills.catalog.dto.SoftSkillDimensionResponseDto;
import ee.kerrete.ainterview.softskills.catalog.service.SoftSkillDimensionResponseMapper;
import ee.kerrete.ainterview.softskills.catalog.service.SoftSkillDimensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/soft-skill/dimensions")
@RequiredArgsConstructor
public class SoftSkillDimensionController {

    private final SoftSkillDimensionService dimensionService;

    @GetMapping
    public List<SoftSkillDimensionResponseDto> list() {
        return SoftSkillDimensionResponseMapper.toDtoList(dimensionService.findAll());
    }
}

