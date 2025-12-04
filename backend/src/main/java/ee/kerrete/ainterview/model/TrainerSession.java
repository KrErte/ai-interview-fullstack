package ee.kerrete.ainterview.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Treeneri sessiooni logi.
 *
 * MVP:
 *  - email + createdAt (statistika jaoks)
 *  - currentQuestion + lastAnswer + lastUpdated (et jätkata sealt, kus pooleli jäi)
 */
@Entity
@Table(name = "trainer_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Kasutaja email, kellele sessioon kuulub.
     */
    @Column(nullable = false)
    private String email;

    /**
     * Viimane treeneri poolt esitatud küsimus,
     * mille juures kasutaja praegu “elab”.
     */
    @Column
    private String currentQuestion;

    /**
     * Kasutaja viimane vastus (mille põhjal järgmine
     * küsimus genereeriti). Praegu ainult logi/taastamise jaoks.
     */
    @Column
    private String lastAnswer;

    /**
     * Millal see sessioon loodi / esimest korda kasutati.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Viimane muutmise aeg (uus küsimus / uus vastus).
     */
    @Column
    private LocalDateTime lastUpdated;
}
