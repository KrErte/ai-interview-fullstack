package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.ProductEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductEventRepository extends JpaRepository<ProductEvent, Long> {

    List<ProductEvent> findByEmailOrderByCreatedAtAsc(String email);

    long countByEmailAndEventType(String email, String eventType);
}
