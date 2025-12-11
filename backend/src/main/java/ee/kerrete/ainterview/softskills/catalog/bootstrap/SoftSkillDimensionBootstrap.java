package ee.kerrete.ainterview.softskills.catalog.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.softskills.catalog.entity.SoftSkillDimension;
import ee.kerrete.ainterview.softskills.catalog.repository.SoftSkillDimensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SoftSkillDimensionBootstrap {

    private static final String RESOURCE_PATH = "softskills/claudesoftskills.json";

    private final ObjectMapper objectMapper;
    private final SoftSkillDimensionRepository repository;

    @Bean
    ApplicationRunner softSkillDimensionsLoader() {
        return args -> loadDimensions();
    }

    @Transactional
    void loadDimensions() {
        Resource resource = new ClassPathResource(RESOURCE_PATH);
        if (!resource.exists()) {
            throw new IllegalStateException("Soft skill catalog resource not found: " + RESOURCE_PATH);
        }
        try {
            byte[] bytes;
            try (InputStream is = resource.getInputStream()) {
                bytes = is.readAllBytes();
            }
            List<ClaudeSoftSkill> items = objectMapper.readValue(bytes, new TypeReference<>() {});
            int inserted = 0;
            int updated = 0;
            for (ClaudeSoftSkill item : items) {
                if (item.key == null || item.key.isBlank()) {
                    throw new IllegalStateException("Soft skill item missing key: " + item);
                }
                SoftSkillDimension existing = repository.findByDimensionKey(item.key.trim().toLowerCase()).orElse(null);
                boolean existed = existing != null && existing.getId() != null;
                SoftSkillDimension dimension = apply(existing != null ? existing : SoftSkillDimension.builder().build(), item);

                repository.save(dimension);
                if (existed) updated++; else inserted++;
            }
            log.info("Soft skill catalog loaded: {} items ({} inserted, {} updated)", items.size(), inserted, updated);
        } catch (Exception e) {
            log.error("Failed to load soft skill catalog from {}: {}", RESOURCE_PATH, e.getMessage(), e);
            throw new IllegalStateException("Unable to load soft skill catalog", e);
        }
    }

    private SoftSkillDimension apply(SoftSkillDimension entity, ClaudeSoftSkill src) {
        String high = toJson(src.highSignals);
        String low = toJson(src.lowSignals);
        String interview = toJson(src.interviewSignals);
        String coaching = toJson(src.coachingIdeas);
        return SoftSkillDimension.builder()
            .id(entity.getId())
            .dimensionKey(src.key.trim().toLowerCase())
            .label(src.label)
            .definition(src.definition)
            .highSignals(high)
            .lowSignals(low)
            .interviewSignals(interview)
            .coachingIdeas(coaching)
            .createdAt(Optional.ofNullable(entity.getCreatedAt()).orElse(Instant.now()))
            .build();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private record ClaudeSoftSkill(
        String key,
        String label,
        String definition,
        List<String> highSignals,
        List<String> lowSignals,
        List<String> interviewSignals,
        List<String> coachingIdeas
    ) { }
}

