// Fail: backend/src/main/java/ee/krerte/aiinterview/repository/PersonalityProfileRepository.java

package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.PersonalityProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalityProfileRepository extends JpaRepository<PersonalityProfile, Long> {
    PersonalityProfile findByEmail(String email);
}
