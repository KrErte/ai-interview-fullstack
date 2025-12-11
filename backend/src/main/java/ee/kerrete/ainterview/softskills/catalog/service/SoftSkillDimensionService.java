package ee.kerrete.ainterview.softskills.catalog.service;

import ee.kerrete.ainterview.softskills.catalog.entity.SoftSkillDimension;
import ee.kerrete.ainterview.softskills.catalog.repository.SoftSkillDimensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoftSkillDimensionService {

    private final SoftSkillDimensionRepository repository;

    @Transactional(readOnly = true)
    public List<SoftSkillDimension> findAll() {
        return repository.findAll().stream()
            .sorted(Comparator.comparing(SoftSkillDimension::getLabel, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SoftSkillDimension> findByKey(String key) {
        return repository.findByDimensionKey(key);
    }
}

