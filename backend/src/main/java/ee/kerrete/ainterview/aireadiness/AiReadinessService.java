package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentRequest;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.CategorySummary;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.Features;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.ProfessionSummary;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Question;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AiReadinessService {

    private final AiReadinessCatalog catalog;
    private final AiReadinessScorer scorer;
    private final AiReadinessCoach coach;

    public AiReadinessService(AiReadinessCatalog catalog, AiReadinessScorer scorer, AiReadinessCoach coach) {
        this.catalog = catalog;
        this.scorer = scorer;
        this.coach = coach;
    }

    static final int MAX_SEARCH_RESULTS = 50;

    /** Mis valikulised funktsioonid on selles serveris kasutusel; frontend peidab ülejäänud. */
    public Features features() {
        return new Features(coach.isEnabled());
    }

    /** Käsitsi kirjutatud ametid; O*NET ametid leiab search() kaudu. */
    public List<ProfessionSummary> professions() {
        return catalog.featured().stream().map(this::summary).toList();
    }

    /**
     * Otsib ameti nimest ja alternatiivnimetustest, tõstutundetult. Tühi päring tagastab käsitsi
     * kirjutatud ametid. Järjestus: täpne nimi, nime algus, täpne alternatiivnimetus, nime sõna
     * algus, alternatiivnimetuse algus, nime sisaldus, alternatiivnimetuse sisaldus, kõik päringu
     * sõnad nimes, kõik päringu sõnad alternatiivnimetuses. Sama astme sees eelistatakse ametit,
     * mille oma nimi päringut katab, siis käsitsi kirjutatud ametit, siis tähestikku. Nii ei jää
     * „Heavy and Tractor-Trailer Truck Drivers“ alternatiivnimetuse „Truck Driver“ tõttu
     * ekspedeerijast tahapoole.
     */
    public List<ProfessionSummary> search(String query, int limit) {
        return search(query, null, limit);
    }

    /** Nagu search(query, limit), aga ainult ühe SOC peagrupi ametid; tühi päring loetleb kogu grupi. */
    public List<ProfessionSummary> search(String query, String category, int limit) {
        int max = Math.max(1, Math.min(MAX_SEARCH_RESULTS, limit));
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean anyGroup = category == null || category.isBlank();
        if (q.isEmpty() && anyGroup) {
            return professions().stream().limit(max).toList();
        }
        Set<String> featuredIds = catalog.featured().stream().map(Profession::id).collect(Collectors.toSet());
        record Hit(Profession profession, int rank, int nameOverlap, boolean featured) {
        }
        return catalog.professions().stream()
                .filter(p -> anyGroup || catalog.group(p).map(category::equals).orElse(false))
                .map(p -> new Hit(p, q.isEmpty() ? 0 : rank(p, q), q.isEmpty() ? 0 : nameOverlap(p, q),
                        featuredIds.contains(p.id())))
                .filter(h -> h.rank() < Integer.MAX_VALUE)
                .sorted(Comparator.comparingInt(Hit::rank)
                        .thenComparing(Comparator.comparingInt(Hit::nameOverlap).reversed())
                        .thenComparing(h -> !h.featured())
                        .thenComparing(h -> h.profession().name()))
                .limit(max)
                .map(h -> summary(h.profession()))
                .toList();
    }

    /**
     * 2 when the profession's own name contains the query, 1 when every query word starts a word
     * in that name, 0 when the hit is only an alternate title.
     */
    private static int nameOverlap(Profession p, String q) {
        String name = p.name().toLowerCase(Locale.ROOT);
        if (name.equals(q) || name.startsWith(q) || name.contains(q)) {
            return 2;
        }
        List<String> queryWords = words(q);
        if (!queryWords.isEmpty() && allWordsMatch(queryWords, name)) {
            return 1;
        }
        return 0;
    }

    /** SOC peagrupid koos ametite arvuga, nime järgi; "other" ei kuulu ühtegi gruppi. */
    public List<CategorySummary> categories() {
        Map<String, Integer> counts = new TreeMap<>();
        catalog.professions().forEach(p -> catalog.group(p).ifPresent(g -> counts.merge(g, 1, Integer::sum)));
        return counts.entrySet().stream().map(e -> new CategorySummary(e.getKey(), e.getValue())).toList();
    }

    private static int rank(Profession p, String q) {
        String name = p.name().toLowerCase(Locale.ROOT);
        List<String> alternates = p.alternateTitles().stream().map(a -> a.toLowerCase(Locale.ROOT)).toList();
        if (name.equals(q)) {
            return 0;
        }
        if (name.startsWith(q)) {
            return 1;
        }
        if (alternates.contains(q)) {
            return 2;
        }
        if (Arrays.stream(name.split("[^\\p{L}\\p{N}]+")).anyMatch(w -> w.startsWith(q))) {
            return 3;
        }
        if (alternates.stream().anyMatch(a -> a.startsWith(q))) {
            return 4;
        }
        if (name.contains(q)) {
            return 5;
        }
        if (alternates.stream().anyMatch(a -> a.contains(q))) {
            return 6;
        }
        // several words in any order, e.g. "family doctor" -> "Family Practice Medical Doctor"
        List<String> queryWords = words(q);
        if (queryWords.size() > 1) {
            if (allWordsMatch(queryWords, name)) {
                return 7;
            }
            if (alternates.stream().anyMatch(a -> allWordsMatch(queryWords, a))) {
                return 8;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static List<String> words(String text) {
        return Arrays.stream(text.split("[^\\p{L}\\p{N}]+")).filter(w -> !w.isEmpty()).toList();
    }

    /** Every query word is the start of some word in the text. */
    private static boolean allWordsMatch(List<String> queryWords, String text) {
        List<String> textWords = words(text);
        return queryWords.stream().allMatch(q -> textWords.stream().anyMatch(w -> w.startsWith(q)));
    }

    /** One profession by id (e.g. to preselect it from a job page); empty for unknown or hidden ids. */
    public Optional<ProfessionSummary> summary(String id) {
        return catalog.profession(id).map(this::summary);
    }

    private ProfessionSummary summary(Profession p) {
        return new ProfessionSummary(p.id(), p.name(), p.category(), p.onetCode() == null);
    }

    public List<Question> questions() {
        return catalog.questions();
    }

    public AssessmentResult assess(AssessmentRequest request) {
        Profession profession = catalog.profession(request.professionId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown profession '" + request.professionId() + "'"));
        AssessmentResult result = scorer.score(profession, request.answers());
        if (!request.personalize()) {
            return result;
        }
        return coach.personalNote(result, answerLabels(request.answers()))
                .map(note -> withNote(result, note))
                .orElse(result);
    }

    private Map<String, String> answerLabels(Map<String, String> answers) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (Question q : catalog.questions()) {
            String optionId = answers.get(q.id());
            q.options().stream()
                    .filter(o -> o.id().equals(optionId))
                    .findFirst()
                    .ifPresent(o -> labels.put(q.text(), o.label()));
        }
        return labels;
    }

    private static AssessmentResult withNote(AssessmentResult r, String note) {
        return new AssessmentResult(r.professionId(), r.professionName(), r.replaceabilityScore(),
                r.taskExposureScore(), r.personalAdjustment(), r.band(), r.headline(), r.readinessScore(),
                r.readinessLevel(), r.tasks(), r.humanEdge(), r.aiTools(), r.pivotRoles(), r.generalRoadmap(),
                r.professionRoadmap(), r.totalWeeks(), note);
    }
}
