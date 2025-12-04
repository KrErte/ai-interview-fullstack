package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.model.WorkstyleSession;
import ee.kerrete.ainterview.repository.WorkstyleSessionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WorkstyleSessionService {
    private static final int MAX_QUESTIONS = 10;

    private static final List<String> QUESTION_BANK = List.of(
            "How would you describe your communication style at work?",
            "How do you handle conflict in team settings?",
            "Do you prefer detailed plans or flexibility in tasks?",
            "How do you approach giving and receiving feedback?",
            "What kind of work environment helps you thrive?",
            "Describe your style of collaborating with colleagues.",
            "How do you prioritize your daily tasks?",
            "How do you manage stress in challenging work situations?",
            "How do you handle disagreements with a manager?",
            "How important is work-life balance to you?"
    );

    @Autowired
    private WorkstyleSessionRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private String generateInitialQuestion() {
        return QUESTION_BANK.get(0);
    }

    private String getNextQuestion(int countSoFar) {
        if (countSoFar >= MAX_QUESTIONS) return null;
        return QUESTION_BANK.get(countSoFar % QUESTION_BANK.size());
    }

    private List<Map<String, String>> getQAList(WorkstyleSession session) {
        try {
            if (session.getAnswersJson() == null) return new ArrayList<>();
            return objectMapper.readValue(
                session.getAnswersJson(), new TypeReference<List<Map<String, String>>>() {}
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Transactional
    public WorkstyleSession startSession(String email) {
        WorkstyleSession existing = repository.findByEmailAndCompletedFalse(email);
        if (existing != null) return existing;
        WorkstyleSession session = new WorkstyleSession();
        session.setEmail(email);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setCompleted(false);
        session.setAnswersJson("[]");
        session.setCurrentQuestion(generateInitialQuestion());
        return repository.save(session);
    }

    @Transactional
    public WorkstyleSession answer(UUID sessionId, String answer) {
        WorkstyleSession session = repository.findById(sessionId).orElseThrow();
        List<Map<String, String>> qaList = getQAList(session);
        Map<String, String> qa = new HashMap<>();
        qa.put("question", session.getCurrentQuestion());
        qa.put("answer", answer);
        qaList.add(qa);
        try {
            session.setAnswersJson(objectMapper.writeValueAsString(qaList));
        } catch (Exception e) {
            // fallback: don't update if error
        }
        if (qaList.size() >= MAX_QUESTIONS) {
            session.setCompleted(true);
            session.setCurrentQuestion(null);
        } else {
            session.setCurrentQuestion(getNextQuestion(qaList.size()));
        }
        session.setUpdatedAt(LocalDateTime.now());
        return repository.save(session);
    }

    public Optional<WorkstyleSession> getSession(UUID id) {
        return repository.findById(id);
    }
}
