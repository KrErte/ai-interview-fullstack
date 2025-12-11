package ee.kerrete.ainterview.softskills.service;

import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileView;
import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.enums.SoftSkillSource;
import ee.kerrete.ainterview.softskills.repository.SoftSkillEvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("all")
class SoftSkillMergedProfileViewServiceTest {

    @Autowired
    private SoftSkillMergedProfileViewService mergedProfileViewService;

    @Autowired
    private SoftSkillEvaluationRepository evaluationRepository;

    @BeforeEach
    void clean() {
        evaluationRepository.deleteAll();
    }

    @Test
    void getMergedProfile_aggregatesAverageAndCounts_sortedByDimension() {
        String email = "candidate@example.com";
        evaluationRepository.save(SoftSkillEvaluation.builder()
            .email(email)
            .dimension("communication")
            .source(SoftSkillSource.HR)
            .score(80)
            .comment("Strong communicator")
            .build());
        evaluationRepository.save(SoftSkillEvaluation.builder()
            .email(email)
            .dimension("communication")
            .source(SoftSkillSource.TECH_LEAD)
            .score(60)
            .comment("Needs clearer RFCs")
            .build());
        evaluationRepository.save(SoftSkillEvaluation.builder()
            .email(email)
            .dimension("ownership")
            .source(SoftSkillSource.TEAM_LEAD)
            .score(90)
            .comment("Owns delivery")
            .build());
        evaluationRepository.save(SoftSkillEvaluation.builder()
            .email(email)
            .dimension("stress_management")
            .source(SoftSkillSource.HR)
            .score(70)
            .comment("Handles pressure")
            .build());

        SoftSkillMergedProfileView profile = mergedProfileViewService.getMergedProfile(email);

        assertEquals(email, profile.getEmail());
        assertThat(profile.getDimensions()).hasSize(3);

        // sorted by dimension key
        assertThat(profile.getDimensions())
            .extracting(dim -> dim.getDimensionKey())
            .isSortedAccordingTo(Comparator.naturalOrder());

        var communication = profile.getDimensions().stream()
            .filter(dim -> "communication".equals(dim.getDimensionKey()))
            .findFirst()
            .orElseThrow();
        assertEquals(70.0, communication.getAverageScore(), 0.001);
        assertEquals(2, communication.getRatingCount());

        var ownership = profile.getDimensions().stream()
            .filter(dim -> "ownership".equals(dim.getDimensionKey()))
            .findFirst()
            .orElseThrow();
        assertEquals(90.0, ownership.getAverageScore(), 0.001);
        assertEquals(1, ownership.getRatingCount());
    }

    @Test
    void getMergedProfile_whenNoEvaluations_returnsEmptyDimensions() {
        SoftSkillMergedProfileView profile = mergedProfileViewService.getMergedProfile("candidate@example.com");

        assertEquals("candidate@example.com", profile.getEmail());
        assertThat(profile.getDimensions()).isEmpty();
    }

    @Test
    void getMergedProfile_missingEmail_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> mergedProfileViewService.getMergedProfile("  "));
        assertThat(ex.getStatusCode().value()).isEqualTo(400);
    }
}


