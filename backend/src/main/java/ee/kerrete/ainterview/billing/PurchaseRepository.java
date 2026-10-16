package ee.kerrete.ainterview.billing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    boolean existsByUserIdAndProductAndStatus(Long userId, String product, PurchaseStatus status);
}
