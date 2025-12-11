package ee.kerrete.ainterview.softskills.catalog;

import ee.kerrete.ainterview.softskills.catalog.entity.SoftSkillDimension;
import ee.kerrete.ainterview.softskills.catalog.repository.SoftSkillDimensionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class SoftSkillDimensionBootstrapTest {

    @Autowired
    private SoftSkillDimensionRepository repository;

    @Test
    void catalogIsLoadedFromResource() {
        long count = repository.count();
        assertThat(count).isGreaterThan(0);

        SoftSkillDimension communication = repository.findByDimensionKey("communication").orElse(null);
        assertThat(communication).isNotNull();
        assertThat(communication.getLabel()).isNotBlank();
        assertThat(communication.getDefinition()).isNotBlank();
    }
}

