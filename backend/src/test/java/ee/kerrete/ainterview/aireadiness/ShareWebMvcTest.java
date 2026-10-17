package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /share and /share/image.png: public, preview tags with the person's score, a 1200x630 PNG; real scoring. */
@WebMvcTest(controllers = ShareController.class)
@Import({SecurityConfig.class, AiReadinessCatalog.class, AiReadinessScorer.class, ShareImageRenderer.class, JobPages.class})
@TestPropertySource(properties = "app.public-base-url=https://careerrisk.example")
class ShareWebMvcTest {

    static final String ANSWERS = "routine.20to50,people.some,physical.partly,judgment.sometimes,experience.2to5,"
            + "domain.some,ai_usage.tried,ai_depth.chat,learning.normal,org.unknown,hours.3";
    static final String BODY = "p:accountant;a:" + ANSWERS;

    @Autowired
    MockMvc mvc;
    @Autowired
    AiReadinessCatalog catalog;
    @Autowired
    AiReadinessScorer scorer;

    @MockBean
    JwtService jwtService;
    @MockBean
    AppUserRepository appUserRepository;

    int expectedScore() {
        Map<String, String> answers = ShareLink.parse(BODY).orElseThrow().answers();
        return scorer.score(catalog.profession("accountant").orElseThrow(), answers).replaceabilityScore();
    }

    @Test
    void sharePageHasPreviewTagsWithTheScoreAndSendsThePersonOnToTheResult() throws Exception {
        String encoded = java.net.URLEncoder.encode(BODY, java.nio.charset.StandardCharsets.UTF_8);
        mvc.perform(get("/share").param("d", BODY))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Accountant: " + expectedScore() + "/100 replaceability by AI. What&#39;s yours?")))
                .andExpect(content().string(containsString(
                        "<meta property=\"og:image\" content=\"https://careerrisk.example/share/image.png?d=" + encoded + "\">")))
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex\">")))
                .andExpect(content().string(containsString("https://careerrisk.example/ai-test#share=" + encoded)));
    }

    @Test
    void brokenOrUnknownLinkGoesToTheTest() throws Exception {
        mvc.perform(get("/share").param("d", "p:nope;a:" + ANSWERS)).andExpect(redirectedUrl("https://careerrisk.example/ai-test"));
        mvc.perform(get("/share").param("d", "garbage")).andExpect(redirectedUrl("https://careerrisk.example/ai-test"));
        mvc.perform(get("/share")).andExpect(redirectedUrl("https://careerrisk.example/ai-test"));
    }

    @Test
    void imageIsAPublicCacheable1200x630Png() throws Exception {
        byte[] png = mvc.perform(get("/share/image.png").param("d", BODY))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(img.getWidth()).isEqualTo(1200);
        assertThat(img.getHeight()).isEqualTo(630);

        byte[] generic = mvc.perform(get("/share/image.png").param("d", "garbage"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(generic).isNotEqualTo(png);
        assertThat(ImageIO.read(new ByteArrayInputStream(generic)).getWidth()).isEqualTo(1200);
    }

    @Test
    void parseMatchesTheFrontendFormat() {
        ShareLink link = ShareLink.parse("p:accountant;a:routine.lt20,people.lot;s:30,70").orElseThrow();
        assertThat(link.professionId()).isEqualTo("accountant");
        assertThat(link.answers()).containsExactly(Map.entry("routine", "lt20"), Map.entry("people", "lot"));
        assertThat(link.shares()).isEqualTo(List.of(30, 70));
        assertThat(ShareLink.parse("p:accountant;a:x.y;s:30,60").orElseThrow().shares()).as("sum is not 100").isNull();
        assertThat(ShareLink.parse("p:accountant;a:x.y.z")).isEmpty();
        assertThat(ShareLink.parse("p:<script>;a:x.y")).isEmpty();
        assertThat(ShareLink.parse("x".repeat(601))).isEmpty();
    }
}
