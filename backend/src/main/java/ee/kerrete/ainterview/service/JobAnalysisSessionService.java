package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobAnalysisSessionService {

    private final JobAnalysisSessionRepository repository;

    public JobAnalysisSession saveSession(String email, String jobTitle, String description) {
        JobAnalysisSession session = JobAnalysisSession.builder()
                .email(email)
                .jobTitle(jobTitle)
                .jobDescription(description)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(session);
    }

    public List<JobAnalysisSession> getSessions(String email) {
        return repository.findByEmail(email);
    }
}
