package ee.kerrete.ainterview.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<WaitlistSignup, Long> {

    boolean existsByEmailAndProduct(String email, String product);
}
