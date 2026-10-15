package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.TaskExposure;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Avalikud SEO-lehed /jobs/{slug}: iga ameti aadress, andmed ja ametite rühmitus.
 * Käsitsi kirjutatud ameti slug on tema id (nt "accountant"), O*NET ametil nimest tehtud
 * slug (nt "cooks-restaurant"). "Other" lehte ei saa.
 */
@Component
public class JobPages {

    static final int TOP_TASKS = 6;
    private static final Set<String> NO_PAGE = Set.of("other");

    private final AiReadinessCatalog catalog;
    private final AiReadinessScorer scorer;
    private final String baseUrl;
    private final List<Profession> all;
    private final Map<String, String> slugById = new HashMap<>();
    private final Map<String, Profession> bySlug = new HashMap<>();
    private final Map<String, Profession> byName = new HashMap<>();
    private final Map<String, String> redirects = new HashMap<>();

    /**
     * Ülesanne, mida kokkuvõte nimetab "AI teeb peamiselt seda", peab olema vähemalt nii palju
     * automatiseeritav; muidu nimetaks lause just seda tööosa, mis jääb inimesele (nt koka toiduvalmistamine).
     */
    static final int LEAD_TASK_MIN_AUTOMATION = 40;

    /**
     * Lehe andmed: neutraalne skoor (ilma vastusteta), suurimad ülesanded ja lingitavad seotud ametid.
     * leadTask on kokkuvõttes nimetatav ülesanne või null, kui ükski suur ülesanne pole piisavalt automatiseeritav.
     */
    public record JobPage(Profession profession, String slug, AssessmentResult result, List<TaskExposure> topTasks,
                          int moreTasks, int moreShare, List<Related> related, String group, TaskExposure leadTask) {
    }

    /** Seotud amet; slug on null, kui sellel pole oma lehte. */
    public record Related(String name, String slug) {
    }

    @Autowired
    public JobPages(AiReadinessCatalog catalog, AiReadinessScorer scorer,
                    @Value("${app.public-base-url:${FRONTEND_BASE_URL:http://localhost:4200}}") String baseUrl) {
        this.catalog = catalog;
        this.scorer = scorer;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.all = catalog.professions().stream().filter(p -> !NO_PAGE.contains(p.id())).toList();
        for (Profession p : all) {
            String slug = p.onetCode() == null ? p.id() : slugify(p.name());
            if (bySlug.containsKey(slug)) {
                slug = slug + "-" + slugify(p.onetCode());
            }
            slugById.put(p.id(), slug);
            bySlug.put(slug, p);
            byName.putIfAbsent(p.name().toLowerCase(Locale.ROOT), p);
        }
        catalog.hiddenTitles().forEach((title, id) -> {
            String slug = slugify(title);
            if (!bySlug.containsKey(slug) && slugById.containsKey(id)) {
                redirects.putIfAbsent(slug, slugById.get(id));
            }
        });
    }

    static String slugify(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }

    /** Kõik lehega ametid: käsitsi kirjutatud ees, siis O*NET nime järgi. */
    public List<Profession> all() {
        return all;
    }

    public Optional<Profession> bySlug(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    /**
     * Peidetud O*NET ameti slug (nt "software-developers") annab selle käsitsi kirjutatud ameti slug'i,
     * mis ameti peidab ("software-developer"); lehega slug'id ei suuna kunagi ümber.
     */
    public Optional<String> redirect(String slug) {
        return Optional.ofNullable(redirects.get(slug));
    }

    public String slugOf(String professionId) {
        return slugById.get(professionId);
    }

    /** id -> slug for every page (templates link by id). */
    public Map<String, String> slugs() {
        return Map.copyOf(slugById);
    }

    /** Ametid SOC peagruppide kaupa, grupid ja ametid nime järgi. */
    public Map<String, List<Profession>> groups() {
        Map<String, List<Profession>> grouped = all.stream()
                .collect(Collectors.groupingBy(p -> catalog.group(p).orElse("Other"), TreeMap::new, Collectors.toList()));
        Map<String, List<Profession>> sorted = new LinkedHashMap<>();
        grouped.forEach((group, list) -> sorted.put(group,
                list.stream().sorted(Comparator.comparing(Profession::name)).toList()));
        return sorted;
    }

    /** Absoluutne aadress (canonical, sitemap). */
    public String url(String path) {
        return baseUrl + path;
    }

    public JobPage page(Profession p) {
        AssessmentResult result = scorer.score(p, Map.of());
        List<TaskExposure> byShare = result.tasks().stream()
                .sorted(Comparator.comparingInt(TaskExposure::share).reversed()).toList();
        List<TaskExposure> top = byShare.stream().limit(TOP_TASKS)
                .sorted(Comparator.comparingInt(TaskExposure::automationPercent).reversed()).toList();
        int moreShare = byShare.stream().skip(TOP_TASKS).mapToInt(TaskExposure::share).sum();
        List<Related> related = p.pivotRoles().stream()
                .map(name -> {
                    Profession match = byName.get(name.toLowerCase(Locale.ROOT));
                    return new Related(name, match == null ? null : slugOf(match.id()));
                })
                .toList();
        TaskExposure leadTask = top.stream()
                .filter(t -> t.automationPercent() >= LEAD_TASK_MIN_AUTOMATION)
                .findFirst().orElse(null);
        return new JobPage(p, slugOf(p.id()), result, top, Math.max(0, byShare.size() - TOP_TASKS), moreShare,
                related, catalog.group(p).orElse(null), leadTask);
    }
}
