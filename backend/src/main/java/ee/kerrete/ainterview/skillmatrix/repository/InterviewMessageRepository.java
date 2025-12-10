package ee.kerrete.ainterview.skillmatrix.repository;

import ee.kerrete.ainterview.skillmatrix.entity.InterviewMessageEntity;
import ee.kerrete.ainterview.skillmatrix.entity.InterviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessageEntity, UUID> {

    List<InterviewMessageEntity> findBySessionOrderByCreatedAtAsc(InterviewSessionEntity session);
}


