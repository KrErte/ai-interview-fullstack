package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.model.AnswerEvaluation;
import ee.kerrete.ainterview.model.ProgressSummary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hoiab vastuste hinnanguid mälus (email -> vastuste ajalugu)
 * ja arvutab progressi.
 *
 * Seda teenust kasutavad nii ProgressController / CandidateProfileController
 * kui ka uued treeneri /user/progress endpointid.
 */
@Service
public class SessionService {

    private final ConcurrentHashMap<String, List<AnswerEvaluation>> historyByEmail =
            new ConcurrentHashMap<>();

    /**
     * Lisa uus vastuse hinnang kasutaja ajalukku.
     */
    public void addEvaluation(String email, AnswerEvaluation evaluation) {
        historyByEmail
                .computeIfAbsent(email, e -> new ArrayList<>())
                .add(evaluation);
    }

    /**
     * Põhimeetod progressi arvutamiseks.
     */
    public ProgressSummary getProgress(String email) {
        List<AnswerEvaluation> history =
                historyByEmail.getOrDefault(email, Collections.emptyList());

        if (history.isEmpty()) {
            return ProgressSummary.builder()
                    .totalQuestions(0)
                    .averageScore(0)
                    .lastScore(null)
                    .bestScore(null)
                    .progressPercent(0)
                    .canStartMockInterview(false)
                    .canRequestPdf(false)
                    .build();
        }

        int total = history.size();
        double avg = history.stream()
                .mapToDouble(a -> a.getScore() != null ? a.getScore() : 0)
                .average()
                .orElse(0);

        // viimase vastuse skoor
        AnswerEvaluation lastEval = history.get(history.size() - 1);
        int lastScore = lastEval.getScore() != null ? lastEval.getScore() : 0;

        // parim skoor
        int bestScore = history.stream()
                .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                .max()
                .orElse(0);

        double progressPercent = Math.min(100, avg);

        return ProgressSummary.builder()
                .totalQuestions(total)
                .averageScore(avg)
                .lastScore(lastScore)
                .bestScore(bestScore)
                .progressPercent(progressPercent)
                .canStartMockInterview(total >= 3 && avg >= 60)
                .canRequestPdf(total >= 1)
                .build();
    }

    /**
     * Tagasisobivad aliased olemasolevatele kontrolleritele –
     * vanad meetodinimed jätkavad tööd.
     */
    public ProgressSummary getSummary(String email) {
        return getProgress(email);
    }

    public ProgressSummary getProgressSummary(String email) {
        return getProgress(email);
    }
}
