package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {

    Optional<CompanyProfile> findByNameIgnoreCase(String name);
}

