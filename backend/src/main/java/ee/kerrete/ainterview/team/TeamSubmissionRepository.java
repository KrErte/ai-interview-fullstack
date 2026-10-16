package ee.kerrete.ainterview.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamSubmissionRepository extends JpaRepository<TeamSubmission, Long> {

    long countByTeamId(Long teamId);

    List<TeamSubmission> findByTeamId(Long teamId);

    Optional<TeamSubmission> findByEditTokenHash(String editTokenHash);

    @Modifying
    @Query("delete from TeamSubmission s where s.teamId = :teamId")
    int deleteByTeamId(@Param("teamId") Long teamId);
}
