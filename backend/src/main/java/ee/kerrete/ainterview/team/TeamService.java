package ee.kerrete.ainterview.team;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.AiReadinessCatalog;
import ee.kerrete.ainterview.aireadiness.AiReadinessScorer;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.RiskBand;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.TeamStatus;
import ee.kerrete.ainterview.aireadiness.CatalogModels.GeneralStage;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Question;
import ee.kerrete.ainterview.aireadiness.CatalogModels.QuestionOption;
import ee.kerrete.ainterview.billing.EntitlementService;
import ee.kerrete.ainterview.billing.Feature;
import ee.kerrete.ainterview.team.TeamModels.AnswerDistribution;
import ee.kerrete.ainterview.team.TeamModels.Count;
import ee.kerrete.ainterview.team.TeamModels.CreateTeamRequest;
import ee.kerrete.ainterview.team.TeamModels.CreatedTeam;
import ee.kerrete.ainterview.team.TeamModels.Percent;
import ee.kerrete.ainterview.team.TeamModels.ProfessionGroup;
import ee.kerrete.ainterview.team.TeamModels.TeamInvite;
import ee.kerrete.ainterview.team.TeamModels.TeamReport;
import ee.kerrete.ainterview.team.TeamModels.TeamTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Meeskonnaraport: loomine, anonüümsed vastused ja koond.
 * <p>
 * Anonüümsusreeglid (spec 3. osa):
 * <ul>
 *   <li>Salvestatakse ainult testi sisu ja kuupäev.</li>
 *   <li>Koond avaneb alles {@value #MIN_RESPONDENTS} vastajaga; enne seda ainult loendur.</li>
 *   <li>Ülesanne on koondis ainult siis, kui see on vähemalt {@value #MIN_RESPONDENTS} vastajal.</li>
 *   <li>Ametite kaupa jaotus on ainult gruppidel, kus on vähemalt {@value #MIN_RESPONDENTS} inimest. Kui
 *   ülejäänud vastajaid on 1..4, jäetakse kogu jaotus ära, sest muidu saaks väikese grupi keskmise
 *   lahutades tuletada.</li>
 *   <li>Raport ei tagasta kunagi üksikvastuseid, omaniku e-posti ega tokeneid.</li>
 * </ul>
 */
@Service
public class TeamService {

    public static final int MIN_RESPONDENTS = 5;
    static final int DEFAULT_MAX_MEMBERS = 50;
    static final int TOP_TASKS = 10;
    /** Kui vähemalt nii suur osa ei kasuta AI-d iga nädal, soovitatakse kõigile AI kirjaoskuse koolitust. */
    static final int LITERACY_FOCUS_PERCENT = 40;

    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int CODE_LENGTH = 10;
    private static final List<String> AI_USE_QUESTIONS = List.of("ai_usage", "ai_depth", "learning");
    private static final Set<String> WEEKLY_AI_USE = Set.of("weekly", "daily");
    private static final List<String> READINESS_LEVELS = List.of("Beginner", "Explorer", "Daily user", "Advanced", "Builder");
    private static final TypeReference<Map<String, String>> ANSWERS = new TypeReference<>() {
    };
    private static final TypeReference<List<StoredTask>> TASKS = new TypeReference<>() {
    };

    /** Üks ülesanne vastaja nädalas, nagu see team_submission.tasks JSON-is on. */
    record StoredTask(String name, int share, int automationPercent, boolean observedUse) {
    }

    private final TeamRepository teams;
    private final TeamSubmissionRepository submissions;
    private final AiReadinessCatalog catalog;
    private final ObjectMapper json;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final EntitlementService entitlements;

    @Autowired
    public TeamService(TeamRepository teams, TeamSubmissionRepository submissions, AiReadinessCatalog catalog,
                       ObjectMapper json,
                       @Value("${app.public-base-url:${FRONTEND_BASE_URL:http://localhost:4200}}") String baseUrl,
                       EntitlementService entitlements) {
        this(teams, submissions, catalog, json, baseUrl, Clock.systemDefaultZone(), entitlements);
    }

    TeamService(TeamRepository teams, TeamSubmissionRepository submissions, AiReadinessCatalog catalog,
                ObjectMapper json, String baseUrl, Clock clock, EntitlementService entitlements) {
        this.entitlements = entitlements;
        this.teams = teams;
        this.submissions = submissions;
        this.catalog = catalog;
        this.json = json;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.clock = clock;
    }

    /** Loob meeskonna. Raportitoken tagastatakse ainult siin; DB-sse läheb ainult selle räsi. */
    @Transactional
    public CreatedTeam create(CreateTeamRequest request) {
        String code = newCode();
        String token = newToken();
        Team team = new Team();
        team.setCode(code);
        team.setAdminTokenHash(sha256(token));
        team.setName(request.name().trim());
        team.setOwnerEmail(request.ownerEmail().trim());
        team.setMaxMembers(DEFAULT_MAX_MEMBERS);
        team.setCreatedAt(LocalDateTime.now(clock));
        teams.save(team);
        return new CreatedTeam(team.getName(), code, token, inviteUrl(code), baseUrl + "/teams/report#" + token,
                team.getMaxMembers());
    }

    @Transactional(readOnly = true)
    public Optional<TeamInvite> invite(String code) {
        return teams.findByCode(normalizeCode(code))
                .map(t -> new TeamInvite(t.getName(), submissions.countByTeamId(t.getId()) >= t.getMaxMembers()));
    }

    /**
     * Lisab vastaja anonüümse tulemuse meeskonda või uuendab tema varasemat vastust.
     * editToken (kui see on teada) uuendab sama brauseri varasemat vastust; muidu lisab teamCode uue.
     * Kui meeskond on täis, test töötab ikka, aga vastust ei salvestata.
     */
    @Transactional
    public TeamStatus record(AssessmentResult result, Map<String, String> answers, String teamCode,
                                       String editToken) {
        if (editToken != null && !editToken.isBlank()) {
            Optional<TeamSubmission> existing = submissions.findByEditTokenHash(sha256(editToken));
            if (existing.isPresent()) {
                TeamSubmission s = existing.get();
                fill(s, result, answers);
                submissions.save(s);
                String name = teams.findById(s.getTeamId()).map(Team::getName).orElse(null);
                return new TeamStatus("updated", name, null);
            }
        }
        if (teamCode == null || teamCode.isBlank()) {
            return new TeamStatus("not-found", null, null);
        }
        String code = normalizeCode(teamCode);
        if (teams.lockByCode(code) == 0) {
            return new TeamStatus("not-found", null, null);
        }
        Optional<Team> found = teams.findByCode(code);
        if (found.isEmpty()) {
            return new TeamStatus("not-found", null, null);
        }
        Team team = found.get();
        if (submissions.countByTeamId(team.getId()) >= team.getMaxMembers()) {
            return new TeamStatus("full", team.getName(), null);
        }
        String token = newToken();
        TeamSubmission s = new TeamSubmission();
        s.setTeamId(team.getId());
        s.setEditTokenHash(sha256(token));
        fill(s, result, answers);
        submissions.save(s);
        return new TeamStatus("added", team.getName(), token);
    }

    private void fill(TeamSubmission s, AssessmentResult result, Map<String, String> answers) {
        s.setProfessionId(result.professionId());
        s.setReplaceability(result.replaceabilityScore());
        s.setTaskExposure(result.taskExposureScore());
        s.setReadiness(result.readinessScore());
        s.setBand(result.band().name());
        s.setAnswers(write(answers));
        s.setTasks(write(result.tasks().stream()
                .map(t -> new StoredTask(t.name(), t.share(), t.automationPercent(), t.observedUse()))
                .toList()));
        if (s.getSubmittedOn() == null) {
            // an update (e.g. after moving the sliders) keeps the date of the first answer
            s.setSubmittedOn(LocalDate.now(clock));
        }
    }

    @Transactional(readOnly = true)
    public Optional<TeamReport> report(String adminToken) {
        return findByToken(adminToken).map(team -> report(team, submissions.findByTeamId(team.getId())));
    }

    /** Kustutab meeskonna ja kõik selle vastused. false, kui token on vale. */
    @Transactional
    public boolean delete(String adminToken) {
        Optional<Team> team = findByToken(adminToken);
        team.ifPresent(t -> {
            submissions.deleteByTeamId(t.getId());
            teams.delete(t);
        });
        return team.isPresent();
    }

    private Optional<Team> findByToken(String adminToken) {
        if (adminToken == null || adminToken.isBlank() || adminToken.length() > 128) {
            return Optional.empty();
        }
        return teams.findByAdminTokenHash(sha256(adminToken));
    }

    TeamReport report(Team team, List<TeamSubmission> rows) {
        int n = rows.size();
        String invite = inviteUrl(team.getCode());
        if (n < MIN_RESPONDENTS) {
            return new TeamReport(team.getName(), team.getCode(), invite, n, MIN_RESPONDENTS, team.getMaxMembers(),
                    false, null, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of());
        }
        List<Map<String, String>> answers = rows.stream().map(r -> read(r.getAnswers(), ANSWERS)).toList();
        List<TeamTask> topTasks = topTasks(rows);
        List<Count> stages = stages(rows);
        int notWeekly = percent(answers.stream().filter(a -> !WEEKLY_AI_USE.contains(a.get("ai_usage"))).count(), n);

        return new TeamReport(
                team.getName(), team.getCode(), invite, n, MIN_RESPONDENTS, team.getMaxMembers(), true,
                rows.stream().map(TeamSubmission::getSubmittedOn).min(Comparator.naturalOrder()).orElse(null),
                rows.stream().map(TeamSubmission::getSubmittedOn).max(Comparator.naturalOrder()).orElse(null),
                average(rows, TeamSubmission::getReplaceability),
                average(rows, TeamSubmission::getTaskExposure),
                average(rows, TeamSubmission::getReadiness),
                bands(rows),
                topTasks,
                aiUse(answers),
                readinessLevels(rows),
                stages,
                focus(topTasks, notWeekly, stages),
                professions(rows));
    }

    private List<Percent> bands(List<TeamSubmission> rows) {
        Map<RiskBand, String> labels = Map.of(RiskBand.LOW, "Low", RiskBand.MODERATE, "Moderate",
                RiskBand.HIGH, "High", RiskBand.VERY_HIGH, "Very high");
        List<Percent> result = new ArrayList<>();
        for (RiskBand band : RiskBand.values()) {
            long count = rows.stream().filter(r -> r.getBand().equals(band.name())).count();
            result.add(new Percent(band.name(), labels.get(band), percent(count, rows.size())));
        }
        return result;
    }

    /**
     * Ülesanded, kuhu meeskonna aeg AI tõttu kõige rohkem muutub: Σ osakaal × automatiseeritavus üle kõigi
     * vastajate, protsendina kogu meeskonna tööajast. Ainult ülesanded, mis on vähemalt MIN_RESPONDENTS-il.
     */
    private List<TeamTask> topTasks(List<TeamSubmission> rows) {
        record Acc(double weight, int automation, int respondents, boolean observed) {
        }
        Map<String, Acc> byName = new LinkedHashMap<>();
        for (TeamSubmission row : rows) {
            for (StoredTask t : read(row.getTasks(), TASKS)) {
                double weight = t.share() * t.automationPercent() / 100.0;
                byName.merge(t.name(), new Acc(weight, t.automationPercent(), 1, t.observedUse()),
                        (a, b) -> new Acc(a.weight() + b.weight(), Math.max(a.automation(), b.automation()),
                                a.respondents() + b.respondents(), a.observed() || b.observed()));
            }
        }
        int n = rows.size();
        return byName.entrySet().stream()
                .filter(e -> e.getValue().respondents() >= MIN_RESPONDENTS)
                .map(e -> new TeamTask(e.getKey(), Math.round(e.getValue().weight() / n * 10) / 10.0,
                        e.getValue().automation(), e.getValue().respondents(), e.getValue().observed()))
                .filter(t -> t.teamTimePercent() > 0)
                .sorted(Comparator.comparingDouble(TeamTask::teamTimePercent).reversed())
                .limit(TOP_TASKS)
                .toList();
    }

    private List<AnswerDistribution> aiUse(List<Map<String, String>> answers) {
        List<AnswerDistribution> result = new ArrayList<>();
        for (String id : AI_USE_QUESTIONS) {
            Optional<Question> question = catalog.questions().stream().filter(q -> q.id().equals(id)).findFirst();
            if (question.isEmpty()) {
                continue;
            }
            List<Percent> options = new ArrayList<>();
            for (QuestionOption o : question.get().options()) {
                long count = answers.stream().filter(a -> o.id().equals(a.get(id))).count();
                options.add(new Percent(o.id(), o.label(), percent(count, answers.size())));
            }
            long missing = answers.stream().filter(a -> a.get(id) == null).count();
            if (missing > 0) {
                options.add(new Percent("none", "Not answered", percent(missing, answers.size())));
            }
            result.add(new AnswerDistribution(id, question.get().text(), options));
        }
        return result;
    }

    private List<Percent> readinessLevels(List<TeamSubmission> rows) {
        return READINESS_LEVELS.stream()
                .map(level -> new Percent(level, level, percent(rows.stream()
                        .filter(r -> AiReadinessScorer.readinessLevel(r.getReadiness()).equals(level)).count(), rows.size())))
                .toList();
    }

    /** Mitu inimest on üldise õppetee igas etapis (etapp, kust nad alustavad). */
    private List<Count> stages(List<TeamSubmission> rows) {
        List<GeneralStage> general = catalog.generalRoadmap();
        Map<String, Integer> counts = new LinkedHashMap<>();
        general.forEach(s -> counts.put(s.id(), 0));
        int finished = 0;
        for (TeamSubmission row : rows) {
            Optional<GeneralStage> current = general.stream().filter(s -> row.getReadiness() < s.doneAtReadiness()).findFirst();
            if (current.isPresent()) {
                counts.merge(current.get().id(), 1, Integer::sum);
            } else {
                finished++;
            }
        }
        List<Count> result = new ArrayList<>();
        for (GeneralStage s : general) {
            result.add(new Count(s.id(), stageTitle(s.title()), counts.get(s.id())));
        }
        if (finished > 0) {
            result.add(new Count("done", "All stages covered", finished));
        }
        return result;
    }

    /** Kolm reeglitega tuletatud koolitusfookust (mitte LLM). */
    private List<String> focus(List<TeamTask> topTasks, int notWeeklyPercent, List<Count> stages) {
        List<String> focus = new ArrayList<>();
        if (!topTasks.isEmpty()) {
            focus.add("Start with AI for: " + topTasks.stream().limit(3).map(TeamTask::name)
                    .map(TeamService::trimSentence).collect(Collectors.joining("; ")) + ".");
        }
        if (notWeeklyPercent >= LITERACY_FOCUS_PERCENT) {
            focus.add("Basic AI literacy session for everyone: " + notWeeklyPercent
                    + "% of the team do not use AI at work every week.");
        }
        stages.stream()
                .filter(c -> !c.key().equals("done") && c.count() > 0)
                .max(Comparator.comparingInt(Count::count))
                .flatMap(c -> catalog.generalRoadmap().stream().filter(s -> s.id().equals(c.key())).findFirst())
                // FP-66: the report is anonymous (token link); while the roadmap is locked, no roadmap items in it
                .ifPresent(stage -> focus.add(entitlements.isLocked(Feature.ROADMAP, null)
                        ? "Training content: " + stageTitle(stage.title()) + "."
                        : "Training content: " + stageTitle(stage.title()) + ". "
                        + stage.items().stream().limit(2).collect(Collectors.joining(" "))));
        return focus;
    }

    /**
     * Ametite kaupa, ainult grupid ≥ MIN_RESPONDENTS. Ülejäänud koondatakse "Other professions" ritta,
     * kui neid on ≥ MIN_RESPONDENTS. Kui ülejäänuid on 1..MIN_RESPONDENTS-1, jäetakse kogu jaotus ära.
     */
    private List<ProfessionGroup> professions(List<TeamSubmission> rows) {
        Map<String, List<TeamSubmission>> byProfession = rows.stream()
                .collect(Collectors.groupingBy(TeamSubmission::getProfessionId, LinkedHashMap::new, Collectors.toList()));
        List<ProfessionGroup> groups = new ArrayList<>();
        List<TeamSubmission> rest = new ArrayList<>();
        byProfession.forEach((id, list) -> {
            if (list.size() >= MIN_RESPONDENTS) {
                String name = catalog.profession(id).map(Profession::name).orElse(id);
                groups.add(group(name, list));
            } else {
                rest.addAll(list);
            }
        });
        if (groups.isEmpty() || (!rest.isEmpty() && rest.size() < MIN_RESPONDENTS)) {
            // no group of 5 (only "Other professions" would repeat the totals), or a small rest that
            // could be derived by subtraction
            return List.of();
        }
        groups.sort(Comparator.comparingInt(ProfessionGroup::respondents).reversed()
                .thenComparing(ProfessionGroup::name));
        if (!rest.isEmpty()) {
            groups.add(group("Other professions", rest));
        }
        return groups;
    }

    private static ProfessionGroup group(String name, List<TeamSubmission> rows) {
        return new ProfessionGroup(name, rows.size(), average(rows, TeamSubmission::getReplaceability),
                average(rows, TeamSubmission::getReadiness));
    }

    private static int average(List<TeamSubmission> rows, java.util.function.ToIntFunction<TeamSubmission> field) {
        return (int) Math.round(rows.stream().mapToInt(field).average().orElse(0));
    }

    private static int percent(long count, int total) {
        return total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
    }

    private static String stageTitle(String title) {
        return title.replaceFirst("^\\d+\\.\\s*", "");
    }

    private static String trimSentence(String text) {
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private String inviteUrl(String code) {
        return baseUrl + "/ai-test?team=" + code;
    }

    static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private String newCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(BASE32[random.nextInt(BASE32.length)]);
            }
            if (!teams.existsByCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Could not create a unique team code");
    }

    /** 32 juhuslikku baiti, URL-safe base64 ilma täiteta. */
    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
