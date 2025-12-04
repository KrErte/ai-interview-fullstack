package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Üldine roadmapi ülevaade profiili jaoks.
 * Sisaldab nii journey (variant 1) kui ka AI soovituse (variant 2) infot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapOverviewDto {

    private String email;

    /**
     * Mitmes samm on hetkel "current" (0-based index).
     */
    private int currentStepIndex;

    /**
     * Kõik roadmapi sammud järjestuses.
     */
    private List<RoadmapStepDto> steps;

    /**
     * AI soovitus – pealkiri (Variant 2).
     */
    private String nextActionTitle;

    /**
     * AI soovitus – kirjeldus, miks see samm on mõistlik.
     */
    private String nextActionDescription;

    /**
     * Frontendi jaoks URL / routerLink, kuhu kasutajat suunata.
     * Nt "/trainer" või "/trainer/soft-skills".
     */
    private String nextActionUrl;
}
