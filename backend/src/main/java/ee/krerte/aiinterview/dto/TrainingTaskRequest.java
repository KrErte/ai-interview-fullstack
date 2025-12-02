package ee.krerte.aiinterview.dto;

import lombok.*;

/**
 * Päring ühe treening-taski staatuse / vastuse uuendamiseks.
 *
 * NB! Toetab nii taskKey kui questionKey välja – vana kood (SkillCoach)
 * kasutab questionKey'd, uus UI kasutab taskKey'd.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingTaskRequest {

    private String email;

    /**
     * Eelistatud väli uues loogikas.
     */
    private String taskKey;

    /**
     * Tagasiühilduvus vanale koodile (SkillCoach).
     */
    private String questionKey;

    /**
     * Kas kasutaja märgib taski tehtuks.
     */
    private Boolean completed;

    /**
     * Kasutaja vastuse tekst (avatud vastused).
     */
    private String answerText;

    /**
     * Võimalik skoor (AI või muu loogika poolt).
     */
    private Integer score;

    /**
     * Abimeetod – tagastab kasutatava key (taskKey eelistatud, muidu questionKey).
     */
    public String resolveTaskKey() {
        if (taskKey != null && !taskKey.isBlank()) {
            return taskKey;
        }
        return questionKey;
    }
}
