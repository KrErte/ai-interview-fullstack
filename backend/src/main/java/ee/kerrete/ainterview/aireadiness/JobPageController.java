package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.RiskBand;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.Optional;

/**
 * Avalikud, serveris renderdatud SEO-lehed: /jobs (indeks), /jobs/{slug} (amet), sitemap.xml, robots.txt.
 * Angular-rakendus renderdab brauseris ja otsingumootor näeks seal tühja lehte; need lehed on puhas HTML.
 */
@Controller
public class JobPageController {

    private static final String CACHE = "public, max-age=3600";

    /** Riskiastme nimi ja värv, samad mis testi tulemuse lehel. */
    private static final Map<RiskBand, Map<String, String>> BANDS = Map.of(
            RiskBand.LOW, Map.of("label", "Low", "color", "#3ddc97"),
            RiskBand.MODERATE, Map.of("label", "Moderate", "color", "#ffd23f"),
            RiskBand.HIGH, Map.of("label", "High", "color", "#ff8c42"),
            RiskBand.VERY_HIGH, Map.of("label", "Very high", "color", "#ff4d6d"));

    private final JobPages pages;

    public JobPageController(JobPages pages) {
        this.pages = pages;
    }

    @GetMapping("/jobs")
    public String index(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", CACHE);
        int count = pages.all().size();
        model.addAttribute("groups", pages.groups());
        model.addAttribute("slugs", pages.slugs());
        model.addAttribute("count", count);
        model.addAttribute("canonical", pages.url("/jobs"));
        model.addAttribute("title", "Will AI replace your job? AI exposure for " + count + " occupations | Futureproof");
        model.addAttribute("description", "How exposed is your job to today's AI? Task-level AI exposure, your human edge "
                + "and an AI learning plan for " + count + " occupations. Free, no signup.");
        return "jobs/index";
    }

    @GetMapping("/jobs/{slug}")
    public String job(@PathVariable String slug, Model model, HttpServletRequest request, HttpServletResponse response) {
        Profession profession = pages.bySlug(slug).orElse(null);
        Optional<String> target = profession == null ? pages.redirect(slug) : Optional.empty();
        if (target.isPresent()) {
            // Peidetud O*NET ameti nimi (nt "software-developers") -> seda katva käsitsi kirjutatud ameti leht
            request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.MOVED_PERMANENTLY);
            response.setHeader("Cache-Control", CACHE);
            return "redirect:" + pages.url("/jobs/" + target.get());
        }
        if (profession == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "jobs/not-found";
        }
        JobPages.JobPage page = pages.page(profession);
        Map<String, String> band = BANDS.get(page.result().band());
        int score = page.result().taskExposureScore();
        response.setHeader("Cache-Control", CACHE);
        model.addAttribute("page", page);
        model.addAttribute("band", band);
        model.addAttribute("canonical", pages.url("/jobs/" + page.slug()));
        model.addAttribute("title", "Will AI replace " + profession.name() + "? AI exposure " + score + "/100 | Futureproof");
        model.addAttribute("description", profession.name() + ": today's AI tools can take on about " + score
                + "% of the typical work (" + band.get("label") + " exposure). See which tasks, your human edge and a "
                + "3-phase AI learning plan. Free 11-question test.");
        return "jobs/job";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap(HttpServletResponse response) {
        response.setHeader("Cache-Control", CACHE);
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : new String[]{"/", "/ai-test", "/jobs"}) {
            appendUrl(xml, path);
        }
        pages.all().forEach(p -> appendUrl(xml, "/jobs/" + pages.slugOf(p.id())));
        return xml.append("</urlset>\n").toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots(HttpServletResponse response) {
        response.setHeader("Cache-Control", CACHE);
        return "User-agent: *\nAllow: /\nDisallow: /api/\n\nSitemap: " + pages.url("/sitemap.xml") + "\n";
    }

    private void appendUrl(StringBuilder xml, String path) {
        xml.append("  <url><loc>").append(HtmlUtils.htmlEscape(pages.url(path))).append("</loc></url>\n");
    }
}
