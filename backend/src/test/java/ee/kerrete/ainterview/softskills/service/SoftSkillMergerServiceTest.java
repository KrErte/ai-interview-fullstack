package ee.kerrete.ainterview.softskills.service;

import ee.kerrete.ainterview.softskills.dto.SoftSkillMergedProfileResponse;
import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.entity.SoftSkillMergedProfile;
import ee.kerrete.ainterview.softskills.enums.SoftSkillDimension;
import ee.kerrete.ainterview.softskills.enums.SoftSkillSource;
import ee.kerrete.ainterview.softskills.repository.SoftSkillEvaluationRepository;
import ee.kerrete.ainterview.softskills.repository.SoftSkillMergedProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("all")
class SoftSkillMergerServiceTest {

    private InMemoryEvaluationRepository evaluationRepository;
    private InMemoryMergedProfileRepository mergedProfileRepository;
    private SoftSkillMergerService mergerService;

    @BeforeEach
    void setUp() {
        evaluationRepository = new InMemoryEvaluationRepository();
        mergedProfileRepository = new InMemoryMergedProfileRepository();
        mergerService = new SoftSkillMergerService(evaluationRepository, mergedProfileRepository);
    }

    @Test
    void mergeForUser_averagesScoresPerDimension() {
        String email = "test@example.com";

        evaluationRepository.storage.add(
                SoftSkillEvaluation.builder()
                        .email(email)
                        .dimension("communication")
                        .source(SoftSkillSource.HR)
                        .score(80)
                        .comment("Strong communicator.")
                        .build()
        );
        evaluationRepository.storage.add(
                SoftSkillEvaluation.builder()
                        .email(email)
                        .dimension("communication")
                        .source(SoftSkillSource.TECH_LEAD)
                        .score(60)
                        .comment("Could be clearer in RFCs.")
                        .build()
        );
        evaluationRepository.storage.add(
            SoftSkillEvaluation.builder()
                    .email(email)
                    .dimension("teamwork")
                    .source(SoftSkillSource.TEAM_LEAD)
                    .score(90)
                    .comment("Great collaborator.")
                    .build()
        );

        Optional<SoftSkillMergedProfileResponse> resultOpt = mergerService.mergeForUser(email);
        assertTrue(resultOpt.isPresent(), "Merged profile should be present");

        SoftSkillMergedProfileResponse result = resultOpt.get();
        assertEquals(email, result.getEmail());

        // COMMUNICATION merged score should be average of 80 and 60 -> 70
        int communicationScore = result.getDimensions().stream()
                .filter(d -> d.getDimension() == SoftSkillDimension.COMMUNICATION)
                .findFirst()
                .map(d -> d.getMergedScore() != null ? d.getMergedScore() : 0)
                .orElse(0);
        assertEquals(70, communicationScore);

        // TEAMWORK merged score should be 90
        int teamworkScore = result.getDimensions().stream()
                .filter(d -> d.getDimension() == SoftSkillDimension.TEAMWORK)
                .findFirst()
                .map(d -> d.getMergedScore() != null ? d.getMergedScore() : 0)
                .orElse(0);
        assertEquals(90, teamworkScore);

        // overallScore should be average of 70 and 90 -> 80
        assertEquals(80, result.getOverallScore());
    }

    /**
     * Minimal in-memory implementation of SoftSkillEvaluationRepository
     * for exercising the merge logic without Spring context.
     */
    @SuppressWarnings("NullableProblems")
    private static class InMemoryEvaluationRepository implements SoftSkillEvaluationRepository {
        private final List<SoftSkillEvaluation> storage = new ArrayList<>();

        @Override
        public List<SoftSkillEvaluation> findByEmail(String email) {
            return storage.stream()
                    .filter(e -> email.equals(e.getEmail()))
                    .toList();
        }

        @Override
        public List<SoftSkillEvaluation> findByEmailAndDimension(String email, SoftSkillDimension dimension) {
            return storage.stream()
                    .filter(e -> email.equals(e.getEmail())
                        && dimension != null
                        && dimension.name().equalsIgnoreCase(String.valueOf(e.getDimension())))
                    .toList();
        }

        // --- JpaRepository methods not needed for this unit test are left unimplemented ---

        @Override
        public List<SoftSkillEvaluation> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SoftSkillEvaluation> findAllById(Iterable<java.util.UUID> uuids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SoftSkillEvaluation> List<S> saveAll(Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SoftSkillEvaluation> S saveAndFlush(S entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SoftSkillEvaluation> List<S> saveAllAndFlush(Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllInBatch(Iterable<SoftSkillEvaluation> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<java.util.UUID> uuids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllInBatch() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SoftSkillEvaluation getOne(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SoftSkillEvaluation getById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SoftSkillEvaluation getReferenceById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SoftSkillEvaluation> S save(S entity) {
            storage.add(entity);
            return entity;
        }

        @Override
        public Optional<SoftSkillEvaluation> findById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            return storage.size();
        }

        @Override
        public void deleteById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(SoftSkillEvaluation entity) {
            storage.remove(entity);
        }

        @Override
        public void deleteAllById(Iterable<? extends java.util.UUID> uuids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll(Iterable<? extends SoftSkillEvaluation> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }

        @Override
        public <S extends SoftSkillEvaluation> S insert(S entity) {
            // insert is a newer JpaRepository default; for our purposes it behaves like save
            storage.add(entity);
            return entity;
        }

        @Override
        public <S extends SoftSkillEvaluation> List<S> insert(Iterable<S> entities) {
            List<S> list = new ArrayList<>();
            for (S e : entities) {
                storage.add(e);
                list.add(e);
            }
            return list;
        }
    }

    /**
     * Minimal in-memory implementation of SoftSkillMergedProfileRepository
     * for exercising the merge logic without Spring context.
     */
    @SuppressWarnings("NullableProblems")
    private static class InMemoryMergedProfileRepository implements SoftSkillMergedProfileRepository {

        private SoftSkillMergedProfile profile;

        @Override
        public Optional<SoftSkillMergedProfile> findByEmail(String email) {
            if (profile != null && email.equals(profile.getEmail())) {
                return Optional.of(profile);
            }
            return Optional.empty();
        }

        @Override
        public List<SoftSkillMergedProfile> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SoftSkillMergedProfile> findAllById(Iterable<java.util.UUID> uuids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SoftSkillMergedProfile> List<S> saveAll(Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SoftSkillMergedProfile> S saveAndFlush(S entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SoftSkillMergedProfile> List<S> saveAllAndFlush(Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllInBatch(Iterable<SoftSkillMergedProfile> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<java.util.UUID> uuids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAllInBatch() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SoftSkillMergedProfile getOne(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SoftSkillMergedProfile getById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SoftSkillMergedProfile getReferenceById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SoftSkillMergedProfile> S save(S entity) {
            this.profile = entity;
            return entity;
        }

        @Override
        public Optional<SoftSkillMergedProfile> findById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            return profile == null ? 0 : 1;
        }

        @Override
        public void deleteById(java.util.UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(SoftSkillMergedProfile entity) {
            if (this.profile == entity) {
                this.profile = null;
            }
        }

        @Override
        public void deleteAllById(Iterable<? extends java.util.UUID> uuids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll(Iterable<? extends SoftSkillMergedProfile> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll() {
            this.profile = null;
        }

        @Override
        public <S extends SoftSkillMergedProfile> S insert(S entity) {
            this.profile = entity;
            return entity;
        }

        @Override
        public <S extends SoftSkillMergedProfile> List<S> insert(Iterable<S> entities) {
            List<S> list = new ArrayList<>();
            for (S e : entities) {
                this.profile = e;
                list.add(e);
            }
            return list;
        }
    }
}


