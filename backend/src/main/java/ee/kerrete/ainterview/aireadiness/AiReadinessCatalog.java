package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.aireadiness.CatalogModels.GeneralStage;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import ee.kerrete.ainterview.aireadiness.CatalogModels.ProfessionTask;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Laeb AI valmisoleku testi sisu ja kontrollib käivitusel, et andmed on kooskõlas
 * (ülesannete osakaalud = 100%, automatiseeritavus 0..1, unikaalsed id-d).
 * <p>
 * Ametid tulevad kahest failist: käsitsi kirjutatud professions.json ja O*NET-ist genereeritud
 * professions-onet.json (tools/onet-import/catalog.py). O*NET amet, mille kood on mõne käsitsi
 * kirjutatud ameti onetCodes'is, peidetakse, et sama amet ei oleks kaks korda.
 */
@Component
public class AiReadinessCatalog {

    private final List<Profession> featured;
    private final List<Profession> professions;
    private final Map<String, Profession> professionsById;
    private final List<Question> questions;
    private final List<GeneralStage> generalRoadmap;
    private final Map<String, String> groups;

    @Autowired
    public AiReadinessCatalog(ObjectMapper objectMapper) {
        this(
                read(objectMapper, "ai-readiness/professions.json", new TypeReference<>() { }),
                read(objectMapper, "ai-readiness/professions-onet.json", new TypeReference<>() { }),
                read(objectMapper, "ai-readiness/questions.json", new TypeReference<>() { }),
                read(objectMapper, "ai-readiness/general-roadmap.json", new TypeReference<>() { })
        );
    }

    AiReadinessCatalog(List<Profession> curated, List<Profession> onet, List<Question> questions,
                       List<GeneralStage> generalRoadmap) {
        Set<String> hidden = new HashSet<>();
        curated.forEach(p -> hidden.addAll(p.onetCodes()));
        List<Profession> visibleOnet = onet.stream().filter(p -> !hidden.contains(p.onetCode())).toList();
        // The SOC major group is the first two digits of the code; hidden codes are not in the O*NET file,
        // so a hand-written profession gets the group of any O*NET profession with the same prefix.
        Map<String, String> groupByMajor = new HashMap<>();
        onet.forEach(p -> groupByMajor.put(p.onetCode().substring(0, 2), p.category()));
        this.groups = new HashMap<>();
        visibleOnet.forEach(p -> groups.put(p.id(), p.category()));
        curated.stream()
                .filter(p -> !p.onetCodes().isEmpty())
                .forEach(p -> groups.put(p.id(), groupByMajor.get(p.onetCodes().get(0).substring(0, 2))));

        this.featured = List.copyOf(curated);
        this.professions = Stream.concat(curated.stream(), visibleOnet.stream()).toList();
        this.questions = List.copyOf(questions);
        this.generalRoadmap = List.copyOf(generalRoadmap);
        this.professionsById = new LinkedHashMap<>();
        Set<String> onetCodes = new HashSet<>();
        for (Profession p : professions) {
            validate(p);
            if (professionsById.put(p.id(), p) != null) {
                throw new IllegalStateException("Duplicate profession id: " + p.id());
            }
            if (p.onetCode() != null && !onetCodes.add(p.onetCode())) {
                throw new IllegalStateException("Duplicate O*NET code: " + p.onetCode());
            }
        }
    }

    /** Käsitsi kirjutatud ametid failis olevas järjekorras. */
    public List<Profession> featured() {
        return featured;
    }

    /**
     * Ameti SOC peagrupp (O*NET ametil category, käsitsi kirjutatul esimese onetCodes koodi grupp);
     * tühi, kui ametil gruppi pole (nt "other").
     */
    public Optional<String> group(Profession p) {
        return Optional.ofNullable(groups.get(p.id()));
    }

    /** Kõik valitavad ametid: käsitsi kirjutatud ja nähtavad O*NET ametid. */
    public List<Profession> professions() {
        return professions;
    }

    public Optional<Profession> profession(String id) {
        return Optional.ofNullable(professionsById.get(id));
    }

    public List<Question> questions() {
        return questions;
    }

    public List<GeneralStage> generalRoadmap() {
        return generalRoadmap;
    }

    private static void validate(Profession p) {
        int total = p.tasks().stream().mapToInt(ProfessionTask::share).sum();
        if (total != 100) {
            throw new IllegalStateException("Task shares of '" + p.id() + "' sum to " + total + ", expected 100");
        }
        for (ProfessionTask t : p.tasks()) {
            if (t.automation() < 0 || t.automation() > 1) {
                throw new IllegalStateException("Automation of '" + t.name() + "' must be within 0..1");
            }
        }
    }

    private static <T> T read(ObjectMapper mapper, String path, TypeReference<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + path, e);
        }
    }
}
