package ee.kerrete.ainterview.repository;

import ee.kerrete.ainterview.model.UserMemoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMemoryEntryRepository extends JpaRepository<UserMemoryEntry, Long> {

    List<UserMemoryEntry> findAllByOrderByCreatedAtDesc();

    List<UserMemoryEntry> findTop3ByOrderByCreatedAtDesc();
}

