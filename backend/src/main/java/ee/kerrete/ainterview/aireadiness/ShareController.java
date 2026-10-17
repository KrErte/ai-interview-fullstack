package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.CatalogModels.Profession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Jagamislink eelvaatega: /share?d=... annab LinkedInile, Slackile jt pealkirja ja pildi isiku skooriga ning suunab
 * inimese edasi tavalisele jagatud tulemusele /ai-test#share=... . Midagi ei salvestata: skoor arvutatakse
 * lingi sisust uuesti (sama vorming mis frontendi share-link.ts). Leht on noindex.
 */
@Controller
public class ShareController {

    private static final CacheControl IMAGE_CACHE = CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();

    private final AiReadinessCatalog catalog;
    private final AiReadinessScorer scorer;
    private final ShareImageRenderer renderer;
    private final JobPages pages;
    /** Piltide joonistamine võtab protsessorit: oma aken, üle piiri tuleb üldine pilt. */
    private final LlmRateLimiter imageLimiter = new LlmRateLimiter(120, 5000);

    public ShareController(AiReadinessCatalog catalog, AiReadinessScorer scorer, ShareImageRenderer renderer,
                           JobPages pages) {
        this.catalog = catalog;
        this.scorer = scorer;
        this.renderer = renderer;
        this.pages = pages;
    }

    @GetMapping("/share")
    public String share(@RequestParam(name = "d", required = false) String d, Model model, HttpServletResponse response) {
        Optional<ShareLink> link = ShareLink.parse(d);
        Optional<AssessmentResult> result = link.flatMap(this::score);
        if (result.isEmpty()) {
            return "redirect:" + pages.url("/ai-test");
        }
        AssessmentResult r = result.get();
        String encoded = URLEncoder.encode(d, StandardCharsets.UTF_8);
        response.setHeader("Cache-Control", "public, max-age=3600");
        model.addAttribute("title", r.professionName() + ": " + r.replaceabilityScore() + "/100 replaceability by AI. What's yours?");
        model.addAttribute("description", r.headline() + " Free 3-minute test for 900+ jobs on careerrisk.ee.");
        model.addAttribute("shareUrl", pages.url("/share?d=" + encoded));
        model.addAttribute("imageUrl", pages.url("/share/image.png?d=" + encoded));
        model.addAttribute("targetUrl", pages.url("/ai-test#share=" + encoded));
        return "share";
    }

    @GetMapping(value = "/share/image.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> image(@RequestParam(name = "d", required = false) String d) {
        Optional<AssessmentResult> result = ShareLink.parse(d).flatMap(this::score);
        byte[] png = result.isPresent() && imageLimiter.tryAcquire(LlmRateLimiter.clientKey())
                ? renderer.render(result.get())
                : renderer.generic();
        return ResponseEntity.ok().cacheControl(IMAGE_CACHE).contentType(MediaType.IMAGE_PNG).body(png);
    }

    /** Sama arvutus mis jagatud lingi avamisel: kõigepealt ilma osakaaludeta ülesannete järjekorra jaoks. */
    Optional<AssessmentResult> score(ShareLink link) {
        Optional<Profession> profession = catalog.profession(link.professionId());
        if (profession.isEmpty()) {
            return Optional.empty();
        }
        try {
            AssessmentResult plain = scorer.score(profession.get(), link.answers());
            List<Integer> shares = link.shares();
            if (shares == null || shares.size() != plain.tasks().size()) {
                return Optional.of(plain);
            }
            Map<String, Integer> taskShares = new HashMap<>();
            for (int i = 0; i < shares.size(); i++) {
                taskShares.put(plain.tasks().get(i).name(), shares.get(i));
            }
            return Optional.of(scorer.score(profession.get(), link.answers(), taskShares));
        } catch (RuntimeException e) {
            // unknown answer ids or shares that do not fit: no preview, like a broken link in the app
            return Optional.empty();
        }
    }
}
