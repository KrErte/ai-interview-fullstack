package ee.krerte.aiinterview.service;

import ee.krerte.aiinterview.dto.TrainingProgressDto;
import ee.krerte.aiinterview.model.TrainingProgress;
import ee.krerte.aiinterview.repository.TrainingProgressRepository;
import ee.krerte.aiinterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final TrainingProgressRepository trainingProgressRepository;
    private final TrainingTaskRepository trainingTaskRepository;

    @Transactional(readOnly = true)
    public TrainingProgressDto getProgressForEmail(String email) {
        TrainingProgress progress = trainingProgressRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("No training progress found for email: " + email));

        long totalTasks = trainingTaskRepository.countByEmail(email);
        long completedTasks = trainingTaskRepository.countByEmailAndCompletedIsTrue(email);

        return TrainingProgressDto.fromEntity(progress, completedTasks, totalTasks);
    }
}
