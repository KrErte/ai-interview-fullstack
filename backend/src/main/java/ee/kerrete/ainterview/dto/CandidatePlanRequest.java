package ee.kerrete.ainterview.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

/**
 * Kandidaadi CV + siht (eesmärk / soovitud ametikoht),
 * nt: "Junior Java Developer" või "Fullstack React/Java"
 */
@Getter
@Setter
public class CandidatePlanRequest {

    @NotBlank
    private String cvText; // kogu CV tekst plain tekstina

    @NotBlank
    private String goal;   // eestikeelne või inglise siht: nt "Java backend developer"
}
