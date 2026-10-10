package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.api.JobAnalysisController;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.service.JobAnalysisService;
import ee.kerrete.ainterview.service.JobAnalysisStatsService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FP-28 (QA review): with the real SecurityConfig, JwtAuthenticationFilter and JwtService (prod profile,
 * a proper JWT_SECRET), an ADMIN token forged with the old hardcoded key, which is the same token whether
 * the key is given raw or as base64:&lt;base64&gt;, gets 401. A genuine token still works.
 */
@WebMvcTest(JobAnalysisController.class)
@Import({SecurityConfig.class, JwtService.class})
@ActiveProfiles("prod")
@org.springframework.test.context.TestPropertySource(properties =
        "app.jwt.secret=base64:lG4UV3eRYbrvZ08LRdOt/hKnZDO1P4YsB7WaquqEMznJopgeGKWvmWuFGaWzkxzB")
class ForgedOldKeyTokenWebMvcTest {

    private static final String OLD_KEY = "super-secret-key-change-me-please-1234567890";
    private static final String ADMIN = "admin@example.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private AppUserRepository appUserRepository;
    @MockBean
    private JobAnalysisService jobAnalysisService;
    @MockBean
    private JobAnalysisStatsService jobAnalysisStatsService;

    @BeforeEach
    void admin() {
        when(appUserRepository.findByEmail(ADMIN)).thenReturn(Optional.of(
                AppUser.builder().id(1L).email(ADMIN).password("x").role(UserRole.ADMIN).enabled(true).build()));
        when(jobAnalysisService.analyze(any(JobAnalysisRequest.class))).thenReturn(new JobAnalysisResponse());
    }

    private static String forgedWith(byte[] key) {
        return Jwts.builder().setSubject(ADMIN).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }

    private static MockHttpServletRequestBuilder analyze(String token) {
        return post("/api/job-analysis").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cvText\":\"cv\",\"jobDescription\":\"job\"}");
    }

    @Test
    void adminTokenForgedWithOldKeyGets401() throws Exception {
        byte[] oldKeyBytes = OLD_KEY.getBytes(StandardCharsets.UTF_8);
        // what JWT_SECRET=base64:<base64 of the old key> would have decoded to: the very same bytes
        byte[] viaBase64 = Base64.getDecoder().decode(Base64.getEncoder().encodeToString(oldKeyBytes));

        mockMvc.perform(analyze(forgedWith(oldKeyBytes))).andExpect(status().isUnauthorized());
        mockMvc.perform(analyze(forgedWith(viaBase64))).andExpect(status().isUnauthorized());
        verifyNoInteractions(jobAnalysisService);
    }

    @Test
    void genuineTokenIsAccepted() throws Exception {
        mockMvc.perform(analyze(jwtService.generateToken(ADMIN))).andExpect(status().isOk());
    }
}
