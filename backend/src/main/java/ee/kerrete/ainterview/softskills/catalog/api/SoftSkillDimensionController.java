package ee.kerrete.ainterview.softskills.catalog.api;

import ee.kerrete.ainterview.softskills.catalog.dto.SoftSkillDimensionResponseDto;
import ee.kerrete.ainterview.softskills.catalog.service.SoftSkillDimensionResponseMapper;
import ee.kerrete.ainterview.softskills.catalog.service.SoftSkillDimensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/soft-skills", "/api/soft-skill"})
@RequiredArgsConstructor
public class SoftSkillDimensionController {

    private final SoftSkillDimensionService dimensionService;

    @GetMapping("/dimensions")
    public List<SoftSkillDimensionResponseDto> list() {
        return SoftSkillDimensionResponseMapper.toDtoList(dimensionService.findAll());
    }

    @GetMapping("/dimensions/{key}")
    public ResponseEntity<SoftSkillDimensionResponseDto> getOne(@PathVariable String key) {
        return dimensionService.findByKey(key)
            .map(SoftSkillDimensionResponseMapper::toDto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

