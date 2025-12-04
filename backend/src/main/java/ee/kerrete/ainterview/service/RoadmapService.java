package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final RoadmapTaskRepository repo;

    public List<RoadmapTask> getTasksForEmail(String email) {
        return repo.findByEmail(email);
    }

    public List<RoadmapTask> updateTask(String email, String taskKey, boolean done) {
        RoadmapTask task = repo.findByEmail(email)
                .stream()
                .filter(t -> t.getTaskKey().equals(taskKey))
                .findFirst()
                .orElseThrow();

        task.setCompleted(done);
        repo.save(task);

        return repo.findByEmail(email);
    }
}
