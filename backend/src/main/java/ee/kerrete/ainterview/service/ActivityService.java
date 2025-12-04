package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final TrainingProgressRepository trainingProgressRepository;

    /**
     * Märgib kasutaja viimase aktiivsuse.
     *
     * Uuendab TrainingProgress.lastActivityAt = LocalDateTime.now()
     * antud emaili jaoks, kui vastav rida eksisteerib.
     *
     * NB! Ei loo uut TrainingProgress rida – kui progressi ei ole,
     * tehakse no-op, et mitte rikkuda võimalikke NOT NULL veerge.
     */
    public void markActivity(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        trainingProgressRepository.findByEmail(email).ifPresent(progress -> {
            progress.setLastActivityAt(LocalDateTime.now());
            trainingProgressRepository.save(progress);
        });
    }
}
