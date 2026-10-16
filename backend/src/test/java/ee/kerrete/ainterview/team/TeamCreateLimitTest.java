package ee.kerrete.ainterview.team;

import ee.kerrete.ainterview.team.TeamModels.CreatedTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/teams: 5 creations per client and 30 in total per hour, then 429. A fresh controller per test. */
class TeamCreateLimitTest {

    TeamService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(TeamService.class);
        when(service.create(any())).thenReturn(new CreatedTeam("Finance", "ABCDEFGHJK", "secret-token",
                "https://x/ai-test?team=ABCDEFGHJK", "https://x/teams/r/secret-token", 50));
        mvc = MockMvcBuilders.standaloneSetup(new TeamController(service, 5, 30)).build();
    }

    @Test
    void sixthCreationFromOneClientIs429() throws Exception {
        for (int i = 0; i < 5; i++) {
            create("203.0.113.7").andExpect(status().isCreated());
        }
        create("203.0.113.7")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("rate_limited"))
                .andExpect(jsonPath("$.message").value("Too many team reports were created. Try again in an hour."));
        create("203.0.113.8").andExpect(status().isCreated());
        verify(service, times(6)).create(any());
    }

    @Test
    void thirtyFirstCreationOverallIs429() throws Exception {
        for (int i = 0; i < 30; i++) {
            create("198.51.100." + i).andExpect(status().isCreated());
        }
        create("198.51.100.200").andExpect(status().isTooManyRequests());
        verify(service, times(30)).create(any());
    }

    private ResultActions create(String remoteAddr) throws Exception {
        return mvc.perform(post("/api/teams")
                .with(request -> {
                    request.setRemoteAddr(remoteAddr);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Finance\",\"ownerEmail\":\"owner@example.org\"}"));
    }
}
