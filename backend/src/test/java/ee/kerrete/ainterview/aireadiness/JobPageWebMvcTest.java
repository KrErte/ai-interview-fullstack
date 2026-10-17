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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Job pages are public (no login), server-rendered HTML with SEO tags; real SecurityConfig. */
@WebMvcTest(controllers = JobPageController.class)
@Import({SecurityConfig.class, ee.kerrete.ainterview.billing.BillingSettings.class, ee.kerrete.ainterview.billing.EntitlementService.class, AiReadinessCatalog.class, AiReadinessScorer.class, JobPages.class})
@TestPropertySource(properties = "app.public-base-url=https://futureproof.example")
class JobPageWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtService jwtService;
    @MockBean
    AppUserRepository appUserRepository;
    @MockBean
    ee.kerrete.ainterview.billing.PurchaseRepository purchaseRepository;

    @Test
    void jobPageIsPublicHtmlWithSeoTagsAndTestLink() throws Exception {
        mvc.perform(get("/jobs/cooks-restaurant"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(content().string(containsString("<title>Will AI replace Cooks, Restaurant?")))
                .andExpect(content().string(containsString(
                        "<link rel=\"canonical\" href=\"https://futureproof.example/jobs/cooks-restaurant\"")))
                .andExpect(content().string(containsString("<meta name=\"description\"")))
                .andExpect(content().string(containsString("href=\"/ai-test?profession=onet-35-2014-00\"")))
                .andExpect(content().string(containsString("data-umami-event=\"seo-cta\"")))
                .andExpect(content().string(containsString("O*NET 29.3")))
                .andExpect(content().string(containsString("<script type=\"application/ld+json\">[{\"@context\":\"https://schema.org\",\"@type\":\"FAQPage\"")))
                .andExpect(content().string(containsString("\"@type\":\"BreadcrumbList\"")));
    }

    @Test
    void handWrittenJobPagesShowTheAiInterviewQuestionsAndOnetPagesDoNot() throws Exception {
        mvc.perform(get("/jobs/accountant"))
                .andExpect(content().string(containsString("AI questions you may get in an interview")))
                .andExpect(content().string(containsString("Walk us through how you would use AI for bookkeeping entries and invoice processing.")));
        mvc.perform(get("/jobs/cooks-restaurant"))
                .andExpect(content().string(not(containsString("AI questions you may get in an interview"))));
    }

    @Test
    void summaryDoesNotClaimAiDoesTheHandsOnCore() throws Exception {
        mvc.perform(get("/jobs/cooks-restaurant"))
                .andExpect(content().string(not(containsString("mostly: season and cook food"))))
                .andExpect(content().string(containsString("None of the largest tasks is highly automatable")));
        mvc.perform(get("/jobs/data-entry-keyers"))
                .andExpect(content().string(containsString("of the typical work in this occupation, mostly: ")))
                .andExpect(content().string(not(containsString("None of the largest tasks"))));
    }

    @Test
    void handWrittenProfessionHasAPage() throws Exception {
        mvc.perform(get("/jobs/accountant"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Will AI replace Accountant?")))
                .andExpect(content().string(not(containsString("O*NET 29.3, U.S. Department of Labor"))));
    }

    @Test
    void hiddenOnetTitleIsPermanentlyRedirectedToTheHandWrittenPage() throws Exception {
        mvc.perform(get("/jobs/software-developers"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://futureproof.example/jobs/software-developer"));
    }

    @Test
    void unknownJobIs404Html() throws Exception {
        mvc.perform(get("/jobs/no-such-job"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("href=\"/jobs\"")));
    }

    @Test
    void indexListsAllJobs() throws Exception {
        mvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/jobs/cooks-restaurant\"")))
                .andExpect(content().string(containsString("href=\"/jobs/accountant\"")))
                .andExpect(content().string(containsString("Food Preparation and Serving Related")));
    }

    @Test
    void sitemapAndRobots() throws Exception {
        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(containsString("<loc>https://futureproof.example/jobs/cooks-restaurant</loc>")))
                .andExpect(content().string(containsString("<loc>https://futureproof.example/ai-test</loc>")))
                .andExpect(content().string(containsString("<loc>https://futureproof.example/why</loc>")));
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sitemap: https://futureproof.example/sitemap.xml")));
    }
}
