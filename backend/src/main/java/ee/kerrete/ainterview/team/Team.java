package ee.kerrete.ainterview.team;

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
 * Meeskond, mille liikmed teevad testi kutselingiga. Raportitoken ise DB-s ei ole, ainult selle SHA-256.
 */
@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kutselingi kood, 10 märki base32. */
    @Column(name = "code", nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "admin_token_hash", nullable = false, unique = true, length = 64)
    private String adminTokenHash;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Arve ja kontakti jaoks; vastajatele ei näidata ja raporti API ei tagasta. */
    @Column(name = "owner_email", nullable = false, length = 254)
    private String ownerEmail;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
