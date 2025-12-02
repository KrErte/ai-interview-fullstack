package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.CreateInterviewSessionRequest;
import ee.krerte.aiinterview.dto.CreateInterviewSessionResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class InterviewSessionService {

    // Lihtne in-memory ID generaator
    private final AtomicLong idSequence = new AtomicLong(1L);

    public CreateInterviewSessionResponse createSession(CreateInterviewSessionRequest request) {
        long id = idSequence.getAndIncrement();

        // praegu lihtsalt tagastame id + email
        // hiljem saame siia lisada küsimuste genereerimise, salvestamise jms
        CreateInterviewSessionResponse response = new CreateInterviewSessionResponse();
        response.setSessionId(id);
        response.setEmail(request.getEmail());
        return response;
    }
}
