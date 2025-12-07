package ee.kerrete.ainterview.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Päring ühe treening-taski staatuse / vastuse uuendamiseks.
 *
 * Seda DTO-d kasutavad:
 *  - MindsetRoadmapController
 *  - SkillCoachController
 *  - TrainingProgressController
 *  - TrainingTaskController
 *
 * NB! Kasutame siin:
 *  - answer         – üldine vastuse tekst (nt SkillCoachilt)
 *  - answerText     – alternatiivne väli (TrainingTaskController kasutab getAnswerText())
 *  - completed      – Boolean, et kontroller saaks kasutada getCompleted()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingTaskRequest {

    @Email
    @NotBlank
    private String email;

    /**
     * Unikaalne võti konkreetse treening-taski jaoks (nt "mindset.conflict-resolution.1").
     */
    @NotBlank
    private String taskKey;

    /**
     * Soft-skilli/oskuse võti, mille alla treening kuulub (nt "conflict_management").
     */
    private String skillKey;

    /**
     * Küsimuse tekst (võib UI-s muutuda).
     */
    private String question;

    /**
     * Vastuse tekst (üldine väli, mida kasutab nt SkillCoachController).
     */
    private String answer;

    /**
     * Vastuse tekst alternatiivse nimega (TrainingTaskController kasutab getAnswerText()).
     */
    private String answerText;

    /**
     * Kas task on lõpetatud (true = valmis).
     * Boolean, et genereeruks getCompleted().
     */
    private Boolean completed;

    /**
     * Skoor (nt 0–100), võib olla null, kui pole veel hinnatud.
     */
    private Integer score;

    /**
     * Abimeetod, mida kontrollerid kutsuvad: request.resolveTaskKey()
     * Praegu tagastame otse taskKey – kui kunagi tahad keerukamat loogikat
     * (nt roadmap + indeksist kokku panna), saab siia panna.
     */
    public String resolveTaskKey() {
        return this.taskKey;
    }
}
