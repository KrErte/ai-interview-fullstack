package ee.kerrete.ainterview.team;

import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.team.TeamModels.CreatedTeam;
import ee.kerrete.ainterview.team.TeamModels.TeamInvite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /api/teams/** is public (no login) through the real SecurityConfig; validation and status codes. */
@WebMvcTest(controllers = TeamController.class)
@Import(SecurityConfig.class)
class TeamControllerWebMvcTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    TeamService service;
    @MockBean
    JwtService jwtService;
    @MockBean
    AppUserRepository appUserRepository;

    @Test
    void createIsPublicAndReturnsTheLinksOnce() throws Exception {
        when(service.create(any())).thenReturn(new CreatedTeam("Finance", "ABCDEFGHJK", "secret-token",
                "https://x/ai-test?team=ABCDEFGHJK", "https://x/teams/r/secret-token", 50));
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Finance\",\"ownerEmail\":\"owner@example.org\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.adminToken").value("secret-token"))
                .andExpect(jsonPath("$.code").value("ABCDEFGHJK"));
    }

    @Test
    void createValidatesNameAndEmail() throws Exception {
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"ownerEmail\":\"owner@example.org\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Finance\",\"ownerEmail\":\"not an email\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "x".repeat(121) + "\",\"ownerEmail\":\"owner@example.org\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    void joinReturnsNameOr404() throws Exception {
        when(service.invite("GOODCODE22")).thenReturn(Optional.of(new TeamInvite("Finance", false)));
        when(service.invite("BADCODE222")).thenReturn(Optional.empty());
        mvc.perform(get("/api/teams/join/GOODCODE22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Finance"))
                .andExpect(jsonPath("$.full").value(false));
        mvc.perform(get("/api/teams/join/BADCODE222")).andExpect(status().isNotFound());
    }

    @Test
    void wrongReportTokenIs404ForReadAndDelete() throws Exception {
        when(service.report(anyString())).thenReturn(Optional.empty());
        when(service.delete(anyString())).thenReturn(false);
        mvc.perform(get("/api/teams/report/wrong")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/teams/report/wrong")).andExpect(status().isNotFound());
    }

    @Test
    void deleteWithTheRightTokenIs204() throws Exception {
        when(service.delete("right")).thenReturn(true);
        mvc.perform(delete("/api/teams/report/right")).andExpect(status().isNoContent());
    }
}
