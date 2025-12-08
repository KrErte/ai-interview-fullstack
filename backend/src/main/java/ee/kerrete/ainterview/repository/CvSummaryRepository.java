package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.CvSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CvSummaryRepository extends JpaRepository<CvSummary, Long> {
    Optional<CvSummary> findByEmail(String email);
    boolean existsByEmail(String email);
}













