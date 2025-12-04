package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Vastuste hindamise teenus:
 *  - proovib esmalt OpenAI hindamist
 *  - kui ebaõnnestub, kasutab lokaalset STAR-fallback hindamist
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EvaluationService {

    private final AiEvaluationClient aiEvaluationClient;

    public EvaluateAnswerResponse evaluate(EvaluateAnswerRequest request) {
        try {
            // Päris GPT hindamine
            return aiEvaluationClient.evaluateStarAnswer(request);
        } catch (RuntimeException e) {
            log.error("OpenAI evaluation failed, using local fallback. Reason: {}", e.getMessage(), e);
            EvaluateAnswerResponse fallback = fallbackEvaluation(request);
            fallback.setFallback(true);
            return fallback;
        }
    }

    /**
     * Lihtne lokaalne heuristika: kontrollib kas vastus sisaldab STAR elemente.
     */
    private EvaluateAnswerResponse fallbackEvaluation(EvaluateAnswerRequest request) {
        String answer = request.getAnswer() == null
                ? ""
                : request.getAnswer().toLowerCase(Locale.ROOT);

        boolean hasSituation = containsAny(answer,
                "situation", "situation:", "olukord", "taust");
        boolean hasTask = containsAny(answer,
                "task", "task:", "ülesanne", "eesmärk");
        boolean hasAction = containsAny(answer,
                "action", "action:", "tegevus", "mida ma tegin", "mida tegin");
        boolean hasResult = containsAny(answer,
                "result", "result:", "tulemus", "mõju", "outcome");

        int parts = 0;
        if (hasSituation) parts++;
        if (hasTask) parts++;
        if (hasAction) parts++;
        if (hasResult) parts++;

        int score = parts * 25;

        StringBuilder strengths = new StringBuilder();
        StringBuilder weaknesses = new StringBuilder();
        StringBuilder suggestions = new StringBuilder();

        if (parts == 0) {
            strengths.append("Vastus sisaldab mingit kogemuslugu, kuid STAR struktuur ei ole äratuntav.");
            weaknesses.append("Puuduvad selged Situation/Task/Action/Result osad.");
            suggestions.append("Proovi vastus üles ehitada nelja blokina: Situation, Task, Action, Result.");
        } else {
            strengths.append("Vastus sisaldab osaliselt STAR struktuuri elemente.");
            if (!hasSituation) {
                weaknesses.append("Puudub selge taust (Situation). ");
                suggestions.append("Alusta lühikese ülevaatega olukorrast ja kontekstist (Situation). ");
            }
            if (!hasTask) {
                weaknesses.append("Ülesanne või eesmärk ei ole eraldi välja toodud (Task). ");
                suggestions.append("Kirjelda, mis oli sinu konkreetne vastutus või eesmärk (Task). ");
            }
            if (!hasAction) {
                weaknesses.append("Tegevused ei ole piisavalt detailsed (Action). ");
                suggestions.append("Räägi samm-sammult, mida just sina tegid (Action). ");
            }
            if (!hasResult) {
                weaknesses.append("Tulemus või mõju jääb ähmaseks (Result). ");
                suggestions.append("Lõpeta vastus mõõdetava tulemuse või mõjuga (Result). ");
            }
        }

        EvaluateAnswerResponse resp = new EvaluateAnswerResponse();
        resp.setScore(score);
        resp.setStrengths(strengths.toString());
        resp.setWeaknesses(weaknesses.toString());
        resp.setSuggestions(suggestions.toString());
        resp.setFallback(true);

        return resp;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
