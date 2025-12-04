package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.CreateInterviewSessionRequest;
import ee.kerrete.ainterview.dto.CreateInterviewSessionResponse;
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
