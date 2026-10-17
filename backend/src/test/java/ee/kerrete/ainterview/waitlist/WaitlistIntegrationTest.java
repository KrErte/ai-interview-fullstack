package ee.kerrete.ainterview.waitlist;

import ee.kerrete.ainterview.config.DataSeeder;
import ee.kerrete.ainterview.config.ProdDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/waitlist: public, validated, one row per email and product, rate limited per client. Liquibase schema on H2. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:waitlist;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.waitlist.per-client-per-hour=4",
        "app.waitlist.global-per-hour=100"
})
@AutoConfigureMockMvc
class WaitlistIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @MockBean
    DataSeeder dataSeeder;
    @MockBean
    ProdDataSeeder prodDataSeeder;

    @BeforeEach
    void empty() {
        jdbc.update("DELETE FROM waitlist_signup");
    }

    ResultActions join(String body, String remoteAddr) throws Exception {
        return mvc.perform(post("/api/waitlist")
                .with(request -> {
                    request.setRemoteAddr(remoteAddr);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void joinWithoutLoginStoresOneRowPerEmailAndProduct() throws Exception {
        join("{\"email\":\"Pat@Example.org\",\"product\":\"interview-prep\",\"professionId\":\"software-developer\",\"source\":\"result\"}", "10.0.0.1")
                .andExpect(status().isNoContent());
        join("{\"email\":\"pat@example.org\",\"product\":\"interview-prep\",\"source\":\"job-page\"}", "10.0.0.1")
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForList("SELECT email, product, profession_id, source FROM waitlist_signup"))
                .singleElement()
                .isEqualTo(Map.of("email", "pat@example.org", "product", "interview-prep",
                        "profession_id", "software-developer", "source", "result"));
    }

    @Test
    void invalidInputIs400AndStoresNothing() throws Exception {
        join("{\"email\":\"not an email\",\"product\":\"interview-prep\"}", "10.0.0.2").andExpect(status().isBadRequest());
        join("{\"email\":\"pat@example.org\",\"product\":\"something-else\"}", "10.0.0.2").andExpect(status().isBadRequest());
        join("{\"email\":\"pat@example.org\",\"product\":\"interview-prep\",\"professionId\":\"<script>\"}", "10.0.0.2")
                .andExpect(status().isBadRequest());
        join("{\"product\":\"interview-prep\"}", "10.0.0.2").andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM waitlist_signup", Integer.class)).isZero();
    }

    @Test
    void fifthJoinFromOneClientIs429() throws Exception {
        for (int i = 0; i < 4; i++) {
            join("{\"email\":\"p" + i + "@example.org\",\"product\":\"interview-prep\"}", "10.0.0.3").andExpect(status().isNoContent());
        }
        join("{\"email\":\"p9@example.org\",\"product\":\"interview-prep\"}", "10.0.0.3")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("rate_limited"));
        join("{\"email\":\"p9@example.org\",\"product\":\"interview-prep\"}", "10.0.0.4").andExpect(status().isNoContent());
    }

    @Test
    void onlyPostIsPublic() throws Exception {
        mvc.perform(get("/api/waitlist")).andExpect(status().isUnauthorized());
    }
}
