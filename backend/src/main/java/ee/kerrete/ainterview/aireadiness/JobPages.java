package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.TaskExposure;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

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
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> NO_PAGE = Set.of("other");

    private final AiReadinessCatalog catalog;
    private final AiReadinessScorer scorer;
    private final String baseUrl;
    private final List<Profession> all;
    private final Map<String, String> slugById = new HashMap<>();
    private final Map<String, Profession> bySlug = new HashMap<>();
    private final Map<String, Profession> byName = new HashMap<>();
    private final Map<String, String> redirects = new HashMap<>();
    private final Map<String, List<InterviewQuestion>> interviewQuestions;

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

    /** AI-oskuse intervjuuküsimus ametilehel: küsimus, mida sellega hinnatakse, ja mida tugev vastus sisaldab. */
    public record InterviewQuestion(String question, String lookFor, String strongAnswer) {
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
        interviewQuestions = readInterviewQuestions();
        interviewQuestions.keySet().forEach(id -> {
            if (!slugById.containsKey(id)) {
                throw new IllegalStateException("interview-questions.json: no job page for profession '" + id + "'");
            }
        });
    }

    private static Map<String, List<InterviewQuestion>> readInterviewQuestions() {
        try (InputStream in = new ClassPathResource("ai-readiness/interview-questions.json").getInputStream()) {
            return new ObjectMapper().readValue(in, new TypeReference<Map<String, List<InterviewQuestion>>>() { });
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load ai-readiness/interview-questions.json", e);
        }
    }

    /** Ameti AI-oskuse intervjuuküsimused; tühi, kui ametile pole neid kirjutatud (praegu O*NET ametid). */
    public List<InterviewQuestion> interviewQuestions(Profession p) {
        return interviewQuestions.getOrDefault(p.id(), List.of());
    }

    /**
     * Käsitsi kirjutatud küsimused, kui need on; muidu samad kuus küsimust ameti enda ülesannetest ja esimesest
     * AI-tööriistast, üldise vea- ja andmelausega (intervjuuks valmistumise leht, kõik ametid).
     */
    public List<InterviewQuestion> interviewQuestionsOrGeneric(Profession p) {
        List<InterviewQuestion> written = interviewQuestions(p);
        if (!written.isEmpty() || p.tasks().isEmpty()) {
            return written;
        }
        var top = p.tasks().stream().max(Comparator.comparingDouble(t -> t.share() * t.automation())).orElseThrow();
        var human = p.tasks().stream()
                .min(Comparator.comparingDouble((CatalogModels.ProfessionTask t) -> t.automation())
                        .thenComparing(t -> -t.share()))
                .orElseThrow();
        String tool = p.aiTools().isEmpty() ? "a general AI assistant such as ChatGPT or Claude" : p.aiTools().get(0);
        return List.of(
                new InterviewQuestion("How do you use AI in your work today?",
                        "That you use AI on purpose, with real examples, not just out of curiosity.",
                        "Name a specific tool (for example " + tool + "), one real task such as “" + top.name()
                                + "”, roughly how much time it saves, and what you still check yourself."),
                new InterviewQuestion("Walk us through how you would use AI for " + lowerFirst(top.name()) + ".",
                        "Your actual workflow: what you give the tool, how you check the result and when you redo it.",
                        "Describe the steps: the context you provide, the first draft from the AI, how you verify it and "
                                + "what you change. Give one example with the time it took before and after."),
                new InterviewQuestion("Tell us about a time AI gave you a wrong or poor result. What did you do?",
                        "Judgment and accountability: you own the result, not the tool.",
                        "A concrete case, for example an AI draft that looked right but contained a factual error or "
                                + "missed something important. Say how you noticed it and what you now do differently."),
                new InterviewQuestion("Where would you not use AI in this job, and why?",
                        "That you know the limits and where people matter most.",
                        "Name the part where human judgment counts most, such as “" + human.name()
                                + "”, and explain what could go wrong if a tool did it."),
                new InterviewQuestion("How do you handle confidential data when you use AI tools?",
                        "That the company can trust you with its own and its customers' data.",
                        "Work documents and customer information are often confidential. Say that you use the tools your "
                                + "employer has approved and remove or anonymise personal data before you share anything."),
                new InterviewQuestion("How has the way you use AI changed in the last six months?",
                        "That you keep learning: AI fluency rubrics look at how your use has developed.",
                        "Show a progression: from one-off prompts to a repeatable workflow, a template or an automation, "
                                + "and the result it gave."));
    }

    private static String lowerFirst(String s) {
        return s.length() > 1 && Character.isLowerCase(s.charAt(1))
                ? Character.toLowerCase(s.charAt(0)) + s.substring(1) : s;
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

    /** Sama methodology lause, mis on lehe jaluses nähtav. */
    static final String METHOD_SENTENCE = "A high score means those tasks will change, not that the job disappears.";

    /**
     * Vastus küsimusele "Will AI replace {amet}?" täpselt nende lausetega, mis lehel juba on: skoor,
     * peamine automatiseeritav ülesanne (kui see on piisavalt automatiseeritav), inimlikud tugevused ja
     * metoodika lause.
     */
    public String faqAnswer(JobPage page) {
        Profession p = page.profession();
        StringBuilder answer = new StringBuilder("Not entirely. Today's AI tools can take on about ")
                .append(page.result().taskExposureScore())
                .append("% of the typical work in this occupation");
        if (page.leadTask() != null) {
            answer.append(", mostly: ").append(page.leadTask().name().toLowerCase(Locale.ROOT));
            if (!page.leadTask().name().endsWith(".")) {
                answer.append('.');
            }
        } else {
            answer.append(". None of the largest tasks is highly automatable; AI mostly helps around the core work.");
        }
        if (!p.humanEdge().isEmpty()) {
            answer.append(" The parts that stay human include: ").append(String.join("; ", p.humanEdge())).append('.');
        }
        return answer.append(' ').append(METHOD_SENTENCE).toString();
    }

    /**
     * FAQPage ja BreadcrumbList JSON-LD ({@code <script type="application/ld+json">} jaoks). "<", ">" ja "&"
     * on JSON-escape'itud, nii et ametinimi ega ülesanne ei saa skriptiplokki sulgeda.
     */
    public String jsonLd(JobPage page) {
        String name = page.profession().name();
        String canonical = url("/jobs/" + page.slug());
        Map<String, Object> faq = new LinkedHashMap<>();
        faq.put("@context", "https://schema.org");
        faq.put("@type", "FAQPage");
        faq.put("mainEntity", List.of(Map.of(
                "@type", "Question",
                "name", "Will AI replace " + name + "?",
                "acceptedAnswer", Map.of("@type", "Answer", "text", faqAnswer(page)))));
        Map<String, Object> crumbs = new LinkedHashMap<>();
        crumbs.put("@context", "https://schema.org");
        crumbs.put("@type", "BreadcrumbList");
        crumbs.put("itemListElement", List.of(
                crumb(1, "Jobs", url("/jobs")),
                crumb(2, name, canonical)));
        try {
            return JSON.writeValueAsString(List.of(faq, crumbs))
                    .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> crumb(int position, String name, String item) {
        Map<String, Object> crumb = new LinkedHashMap<>();
        crumb.put("@type", "ListItem");
        crumb.put("position", position);
        crumb.put("name", name);
        crumb.put("item", item);
        return crumb;
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
