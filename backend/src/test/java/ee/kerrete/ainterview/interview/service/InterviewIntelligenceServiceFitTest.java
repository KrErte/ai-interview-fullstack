package ee.kerrete.ainterview.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.interview.dto.InterviewIntelligenceResponseDto;
import ee.kerrete.ainterview.interview.dto.InterviewNextQuestionRequestDto;
import ee.kerrete.ainterview.interview.enums.InterviewerStyle;
import ee.kerrete.ainterview.model.InterviewSession;
import ee.kerrete.ainterview.repository.InterviewSessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SuppressWarnings("null")
class InterviewIntelligenceServiceFitTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final InterviewSessionRepository repo = Mockito.mock(InterviewSessionRepository.class);
    private final InterviewIntelligenceService service = new InterviewIntelligenceService(repo, mapper);

    @Test
    void fitNotComputedBeforeThirdAnswer() throws Exception {
        InterviewSession session = baseSession(1, List.of(4.0));
        UUID uuid = session.getSessionUuid();
        Mockito.when(repo.findBySessionUuid(uuid)).thenReturn(Optional.of(session));
        Mockito.when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InterviewIntelligenceResponseDto resp = service.nextQuestion(uuid, new InterviewNextQuestionRequestDto("ans"));

        assertThat(resp.getFitScore()).isNull();
        assertThat(resp.getFitTrend()).isNull();
        assertThat(resp.getFit().getComputed()).isFalse();
        assertThat(resp.getFit().getOverall()).isNull();
        assertThat(resp.getFit().getTrend()).isNull();
    }

    @Test
    void fitComputedFromThirdAnswer() throws Exception {
        InterviewSession session = baseSession(2, List.of(4.0, 4.0));
        UUID uuid = session.getSessionUuid();
        Mockito.when(repo.findBySessionUuid(uuid)).thenReturn(Optional.of(session));
        Mockito.when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InterviewIntelligenceResponseDto resp = service.nextQuestion(uuid, new InterviewNextQuestionRequestDto("ans"));

        assertThat(resp.getFitScore()).isNotNull();
        assertThat(resp.getFit().getComputed()).isTrue();
        assertThat(resp.getFit().getOverall()).isEqualTo(resp.getFitScore().doubleValue());
    }

    private InterviewSession baseSession(int answered, List<Double> avgs) throws Exception {
        InterviewSession s = InterviewSession.builder()
            .id(1L)
            .sessionUuid(UUID.randomUUID())
            .interviewerStyle(InterviewerStyle.HR)
            .questionAnswers(mapper.writeValueAsString(avgs.stream().map(a ->
                new ScoreStub("a", a, a, a, a)).toList()))
            .questionCount(answered)
            .currentDimension("ownership_accountability")
            .probeCount(0)
            .build();
        return s;
    }

    private record ScoreStub(String answer, double depth, double specificity, double relevance, double avg) { }
}

