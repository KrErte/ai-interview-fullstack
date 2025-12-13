package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.CreateInterviewSessionRequest;
import ee.kerrete.ainterview.dto.CreateInterviewSessionResponse;
import ee.kerrete.ainterview.model.InterviewSession;
import ee.kerrete.ainterview.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private static final String DEFAULT_VALUE = "unspecified";

    private final InterviewSessionRepository interviewSessionRepository;

    @SuppressWarnings("null")
    public CreateInterviewSessionResponse createSession(CreateInterviewSessionRequest request) {
        UUID sessionUuid = UUID.randomUUID();

        InterviewSession entity = InterviewSession.builder()
            .company(DEFAULT_VALUE)
            .role(DEFAULT_VALUE)
            .seniority(null)
            .sessionUuid(sessionUuid)
            .questionAnswers(null)
            .createdAt(LocalDateTime.now())
            .build();

        InterviewSession saved = Objects.requireNonNull(interviewSessionRepository.save(entity));

        CreateInterviewSessionResponse response = new CreateInterviewSessionResponse();
        response.setSessionId(saved.getId());
        response.setEmail(request.getEmail());
        response.setSessionUuid(saved.getSessionUuid());
        return response;
    }
}
