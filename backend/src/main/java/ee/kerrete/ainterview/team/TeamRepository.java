package ee.kerrete.ainterview.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByCode(String code);

    Optional<Team> findByAdminTokenHash(String adminTokenHash);

    boolean existsByCode(String code);

    /**
     * Lukustab meeskonna rea transaktsiooni lõpuni (tühi UPDATE), et kaks samaaegset vastust ei ületaks
     * max_members piiri. Töötab nii PostgreSQL-is kui H2-s, erinevalt SELECT ... FOR UPDATE süntaksist.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Team t set t.maxMembers = t.maxMembers where t.code = :code")
    int lockByCode(@Param("code") String code);
}
