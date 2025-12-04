package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.model.AnswerEvaluation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Hinnang vastusele (0–100) + tekstiline tagasiside.
 * Lihtne heuristiline lahendus – hiljem saab OpenAI peale keerata.
 */
@Service
@RequiredArgsConstructor
public class AnswerEvaluationService {

    private final SessionService sessionService;

    public AnswerEvaluation evaluate(EvaluateAnswerRequest request) {
        String answer = request.getAnswer() == null ? "" : request.getAnswer();
        String lower = answer.toLowerCase();

        double score = 0;
        StringBuilder fb = new StringBuilder();

        // Pikkus
        int length = answer.length();
        if (length < 200) {
            fb.append("Vastus on üsna lühike – proovi anda rohkem detaile. ");
            score += 20;
        } else if (length < 600) {
            fb.append("Vastuse pikkus on enam-vähem hea. ");
            score += 35;
        } else {
            fb.append("Vastus on põhjalik – hea! ");
            score += 45;
        }

        // STAR elemendid
        boolean hasSituation = lower.contains("situation") || lower.contains("olukord");
        boolean hasTask = lower.contains("task") || lower.contains("ülesanne");
        boolean hasAction = lower.contains("action") || lower.contains("tegevus");
        boolean hasResult = lower.contains("result") || lower.contains("tulemus");

        int starParts = 0;
        if (hasSituation) starParts++;
        if (hasTask) starParts++;
        if (hasAction) starParts++;
        if (hasResult) starParts++;

        score += starParts * 10;

        if (starParts < 4) {
            fb.append("Püüa STAR struktuuri selgemalt kasutada (Situation, Task, Action, Result). ");
        } else {
            fb.append("STAR struktuur on selgelt näha – väga hea. ");
        }

        // Numbrid / mõju
        boolean hasNumber = answer.matches("(?s).*\\d+.*");
        if (hasNumber) {
            score += 15;
            fb.append("Hea, et tood välja numbreid ja mõõdetavaid tulemusi. ");
        } else {
            fb.append("Lisa konkreetseid numbreid (nt protsendid, ajavõit, rahaline mõju). ");
        }

        // Piira 0–100
        if (score > 100) score = 100;
        if (score < 0) score = 0;

        int intScore = (int) Math.round(score);

        AnswerEvaluation evaluation = AnswerEvaluation.builder()
                .question(request.getQuestion())
                .answer(answer)
                .score(intScore)
                .feedback(fb.toString().trim())
                .strengths(Collections.emptyList())
                .weaknesses(Collections.emptyList())
                .suggestions(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .build();

        // Salvesta progressi jaoks
        sessionService.addEvaluation(request.getEmail(), evaluation);

        return evaluation;
    }
}
