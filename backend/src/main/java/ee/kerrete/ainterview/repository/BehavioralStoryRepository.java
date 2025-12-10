package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.BehavioralStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BehavioralStoryRepository extends JpaRepository<BehavioralStory, Long> {

    List<BehavioralStory> findAllByOrderByCreatedAtDesc();
}

