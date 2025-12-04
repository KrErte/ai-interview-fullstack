package ee.kerrete.ainterview.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Päring AI põhise arenguplaani genereerimiseks.
 * Treeneri vaade saadab siia:
 *  - email
 *  - Job Matcheri kokkuvõtte teksti
 *  - fookusoskuste listi (chipid Treener vaates).
 */
@Getter
@Setter
public class SkillPlanRequest {

    /**
     * Kasutaja email – seotakse profili ja ajalooga.
     */
    private String email;

    /**
     * Viimase Job Matcheri AI kokkuvõte (sama tekst,
     * mida juba Treeneri vaates kuvad).
     */
    private String jobMatcherSummary;

    /**
     * Oskused, mille kohta kasutaja tahab treeningplaani.
     */
    private List<String> focusSkills;
}
