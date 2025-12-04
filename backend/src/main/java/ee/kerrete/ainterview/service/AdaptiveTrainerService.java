package ee.kerrete.ainterview.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AdaptiveTrainerService – õige nimi, õiges failis.
 * Siia tuleb kogu loogika, mida varem püüdsid AiAdaptiveService alla panna.
 */
@Service
@RequiredArgsConstructor
public class AdaptiveTrainerService {

    /**
     * Päris AI coachi loogika – saad hiljem täita.
     */
    public String askAiCoach(String question, String answer) {
        return "AI Coach placeholder response for: " + question;
    }
}
