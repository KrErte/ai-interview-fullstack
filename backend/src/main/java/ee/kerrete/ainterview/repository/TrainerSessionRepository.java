package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.TrainerSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainerSessionRepository extends JpaRepository<TrainerSession, Long> {

    /**
     * Mitu treeneri sessiooni on antud emailiga salvestatud.
     * (võid kasutada profiili statistikas kui tahad)
     */
    int countByEmail(String email);

    /**
     * Leiab antud emaili viimase sessiooni (kõige uuem createdAt).
     */
    Optional<TrainerSession> findFirstByEmailOrderByCreatedAtDesc(String email);
}
