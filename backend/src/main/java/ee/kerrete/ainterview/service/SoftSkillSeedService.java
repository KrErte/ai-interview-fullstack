package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoftSkillSeedService {

    private final ObjectMapper objectMapper;

    private Map<String, List<SeedQuestion>> seedsByRoadmap = new HashMap<>();

    @PostConstruct
    public void loadSeeds() {
        try {
            ClassPathResource resource = new ClassPathResource("softskills-roadmaps.json");
            seedsByRoadmap = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<Map<String, List<SeedQuestion>>>() {}
            );
            log.info("Loaded soft-skill roadmap seeds for keys: {}", seedsByRoadmap.keySet());
        } catch (IOException e) {
            log.error("Failed to load softskills-roadmaps.json", e);
            seedsByRoadmap = new HashMap<>();
        }
    }

    /**
     * Tagastab järgmise seed-küsimuse antud roadmapKey jaoks, võttes arvesse mitu
     * küsimust on sessioonis juba küsitud.
     */
    public Optional<SeedQuestion> getNextSeed(String roadmapKey, int askedCount) {
        List<SeedQuestion> list = seedsByRoadmap.getOrDefault(roadmapKey, Collections.emptyList());
        if (askedCount < 0 || askedCount >= list.size()) {
            return Optional.empty();
        }
        return Optional.of(list.get(askedCount));
    }

    /**
     * Tagastab, mitu eelseadistatud küsimust antud teema jaoks on.
     */
    public int getSeedCount(String roadmapKey) {
        return seedsByRoadmap.getOrDefault(roadmapKey, Collections.emptyList()).size();
    }

    @Data
    public static class SeedQuestion {
        private int order;
        private String questionText;
        private String difficulty;
    }
}
