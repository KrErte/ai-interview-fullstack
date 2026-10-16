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

import java.time.LocalDate;

/**
 * Üks anonüümne vastus meeskonna koondisse: ainult testi sisu ja kuupäev, ei nime, e-posti ega IP-d.
 */
@Entity
@Table(name = "team_submission")
@Getter
@Setter
@NoArgsConstructor
public class TeamSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /** SHA-256 tokenist, mida hoiab ainult vastaja brauser, et oma vastust uuendada. */
    @Column(name = "edit_token_hash", nullable = false, unique = true, length = 64)
    private String editTokenHash;

    @Column(name = "profession_id", nullable = false, length = 64)
    private String professionId;

    @Column(name = "replaceability", nullable = false)
    private int replaceability;

    @Column(name = "task_exposure", nullable = false)
    private int taskExposure;

    @Column(name = "readiness", nullable = false)
    private int readiness;

    @Column(name = "band", nullable = false, length = 16)
    private String band;

    /** JSON: questionId -> optionId. Raporti API ei tagasta seda kunagi. */
    @Column(name = "answers", nullable = false, columnDefinition = "CLOB")
    private String answers;

    /** JSON: [{name, share, automationPercent, observedUse}]. */
    @Column(name = "tasks", nullable = false, columnDefinition = "CLOB")
    private String tasks;

    @Column(name = "submitted_on", nullable = false)
    private LocalDate submittedOn;
}
