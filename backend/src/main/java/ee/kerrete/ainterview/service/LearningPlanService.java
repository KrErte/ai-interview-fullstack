package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.LearningPlanDto;
import ee.kerrete.ainterview.dto.SaveLearningPlanRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LearningPlanService {

    // Hoiame viimast plaani e-posti kaupa in-memory
    private final ConcurrentHashMap<String, LearningPlanDto> lastPlanByEmail = new ConcurrentHashMap<>();

    public void savePlan(SaveLearningPlanRequest request) {
        LearningPlanDto dto = new LearningPlanDto();
        dto.setEmail(request.getEmail());
        dto.setJobTitle(request.getJobTitle());
        dto.setMissingSkills(request.getMissingSkills());
        dto.setRoadmap(request.getRoadmap());

        lastPlanByEmail.put(request.getEmail(), dto);
    }

    public Optional<LearningPlanDto> getLastPlan(String email) {
        return Optional.ofNullable(lastPlanByEmail.get(email));
    }
}
