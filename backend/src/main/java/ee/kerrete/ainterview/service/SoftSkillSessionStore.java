package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.SoftSkillQuestionRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Väike in-memory sessioonimälu.
 * Võti: sessionId (front võib kasutada email+roadmapKey, või tulevikus eraldi ID).
 */
@Component
public class SoftSkillSessionStore {

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public SessionState getOrCreate(String sessionId, String email, String roadmapKey) {
        return sessions.computeIfAbsent(sessionId, id -> {
            SessionState s = new SessionState();
            s.setSessionId(id);
            s.setEmail(email);
            s.setRoadmapKey(roadmapKey);
            s.setCreatedAt(Instant.now());
            return s;
        });
    }

    public void appendTurn(String sessionId, String question, String answer) {
        SessionState state = sessions.get(sessionId);
        if (state == null) return;
        SoftSkillQuestionRequest.HistoryTurn turn = new SoftSkillQuestionRequest.HistoryTurn();
        turn.setQuestion(question);
        turn.setAnswer(answer);
        state.getHistory().add(turn);
        state.setLastUpdated(Instant.now());
    }

    public List<SoftSkillQuestionRequest.HistoryTurn> getHistory(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null) return Collections.emptyList();
        return new ArrayList<>(state.getHistory());
    }

    @Data
    public static class SessionState {
        private String sessionId;
        private String email;
        private String roadmapKey;
        private Instant createdAt;
        private Instant lastUpdated;
        private List<SoftSkillQuestionRequest.HistoryTurn> history = new ArrayList<>();
    }
}
