package ee.kerrete.ainterview.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LearningPlan {

    private String headline;   // nt "Alusta baaskontseptsioonidest..."
    private String summary;    // lühike kokkuvõte
    private List<String> steps;
}
