package ee.kerrete.ainterview.softskills.repository;

import ee.kerrete.ainterview.softskills.entity.SoftSkillEvaluation;
import ee.kerrete.ainterview.softskills.enums.SoftSkillDimension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SoftSkillEvaluationRepository extends JpaRepository<SoftSkillEvaluation, UUID> {

    List<SoftSkillEvaluation> findByEmail(String email);

    List<SoftSkillEvaluation> findByEmailAndDimension(String email, SoftSkillDimension dimension);

    /**
     * Default stub to keep legacy in-memory test implementations compiling across Spring Data versions.
     */
    @Override
    default <S extends SoftSkillEvaluation> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    /**
     * Default stub for compatibility with older in-memory repositories.
     */
    @Override
    default <S extends SoftSkillEvaluation> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    default List<SoftSkillEvaluation> findAll(Sort sort) {
        return List.of();
    }

    @Override
    default org.springframework.data.domain.Page<SoftSkillEvaluation> findAll(org.springframework.data.domain.Pageable pageable) {
        return org.springframework.data.domain.Page.empty();
    }

    @Override
    default <S extends SoftSkillEvaluation, R> R findBy(Example<S> example,
                                                        java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    default <S extends SoftSkillEvaluation> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    default <S extends SoftSkillEvaluation> long count(Example<S> example) {
        return 0;
    }

    @Override
    default <S extends SoftSkillEvaluation> org.springframework.data.domain.Page<S> findAll(Example<S> example, org.springframework.data.domain.Pageable pageable) {
        return org.springframework.data.domain.Page.empty();
    }

    @Override
    default <S extends SoftSkillEvaluation> java.util.Optional<S> findOne(Example<S> example) {
        return java.util.Optional.empty();
    }

    default <S extends SoftSkillEvaluation> S insert(S entity) {
        return save(entity);
    }

    default <S extends SoftSkillEvaluation> List<S> insert(Iterable<S> entities) {
        return saveAll(entities);
    }
}


