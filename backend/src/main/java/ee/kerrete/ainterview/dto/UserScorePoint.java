package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ühe analüüsi punkt graafikul (viimased skoorid profiilivaates).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserScorePoint {

    /**
     * Silt graafikul (nt "#1", "#2" või kuupäev).
     */
    private String label;

    /**
     * Sobivuse skoor 0–100.
     */
    private double score;
}
