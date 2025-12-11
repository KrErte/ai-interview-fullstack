package ee.kerrete.ainterview.softskills.service;

import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationRequest;
import ee.kerrete.ainterview.softskills.dto.SoftSkillEvaluationResponse;
import ee.kerrete.ainterview.softskills.enums.SoftSkillSource;
import ee.kerrete.ainterview.softskills.repository.SoftSkillEvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
@Transactional
class SoftSkillEvaluationServiceDimensionTest {

    @Autowired
    private SoftSkillEvaluationService evaluationService;

    @Autowired
    private SoftSkillEvaluationRepository evaluationRepository;

    @BeforeEach
    void clean() {
        evaluationRepository.deleteAll();
    }

    @Test
    void createEvaluation_resolvesDimensionKey() {
        SoftSkillEvaluationRequest request = SoftSkillEvaluationRequest.builder()
            .email("candidate@example.com")
            .sourceType(SoftSkillSource.HR.name())
            .scores(java.util.List.of(
                SoftSkillEvaluationRequest.SoftSkillScoreRequest.builder()
                    .dimensionKey("communication")
                    .score(90)
                    .explanation("Great communicator")
                    .build()
            ))
            .build();

        java.util.List<SoftSkillEvaluationResponse> responses = evaluationService.createEvaluations(request);

        assertThat(responses).hasSize(1);
        SoftSkillEvaluationResponse response = responses.get(0);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getDimension()).isNotNull();
        assertThat(response.getDimension()).isEqualTo("communication");
    }
}

