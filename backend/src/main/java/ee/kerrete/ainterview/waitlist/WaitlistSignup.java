package ee.kerrete.ainterview.waitlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ootelisti kirje toote jaoks, mida veel ei müüda (võltsuks). E-post on ainult selleks, et teatada üks kord, kui
 * toode valmib; see ei ole konto ega seotud testi vastustega.
 */
@Entity
@Table(name = "waitlist_signup")
@Getter
@Setter
@NoArgsConstructor
public class WaitlistSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "product", nullable = false, length = 32)
    private String product;

    /** Amet, mille tulemuselt või ametilehelt liituti; võib puududa. */
    @Column(name = "profession_id", length = 128)
    private String professionId;

    /** Kust liituti, nt "result" või "job-page". */
    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
