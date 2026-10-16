package ee.kerrete.ainterview.team;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Meeskonnaraporti API päringud ja vastused. Raporti vastus ei sisalda kunagi ühe vastaja andmeid,
 * omaniku e-posti ega tokeneid.
 */
public final class TeamModels {

    private TeamModels() {
    }

    public record CreateTeamRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 254) String ownerEmail
    ) {
    }

    /** adminToken näidatakse ainult üks kord; DB-s on ainult selle räsi. */
    public record CreatedTeam(String name, String code, String adminToken, String inviteUrl, String reportUrl,
                              int maxMembers) {
    }

    /** Kutsebänneri jaoks: meeskonna nimi ja kas see on täis. */
    public record TeamInvite(String name, boolean full) {
    }

    public record Percent(String key, String label, int percent) {
    }

    public record Count(String key, String label, int count) {
    }

    /** Meeskonna ülesanne: osa kogu meeskonna tööajast, mida AI võiks teha, ja mitmel vastajal see on. */
    public record TeamTask(String name, double teamTimePercent, int averageAutomation, int respondents,
                           boolean observedUse) {
    }

    public record AnswerDistribution(String questionId, String question, List<Percent> options) {
    }

    public record ProfessionGroup(String name, int respondents, int averageReplaceability, int averageReadiness) {
    }

    /**
     * Koondraport. ready=false, kui vastajaid on alla minRespondents: siis on täidetud ainult päis
     * ja loendur, kõik muu on tühi.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TeamReport(
            String teamName,
            String code,
            String inviteUrl,
            int respondents,
            int minRespondents,
            int maxMembers,
            boolean ready,
            LocalDate firstDate,
            LocalDate lastDate,
            Integer averageReplaceability,
            Integer averageTaskExposure,
            Integer averageReadiness,
            List<Percent> bands,
            List<TeamTask> topTasks,
            List<AnswerDistribution> aiUse,
            List<Percent> readinessLevels,
            List<Count> stages,
            List<String> focus,
            List<ProfessionGroup> professions
    ) {
    }
}
