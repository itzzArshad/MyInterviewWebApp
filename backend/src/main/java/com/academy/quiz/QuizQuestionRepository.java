package com.academy.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByConceptId(Long conceptId);

    List<QuizQuestion> findByConceptIdAndActiveTrueOrderBySortOrder(Long conceptId);

    long countByConceptIdAndActiveTrue(Long conceptId);

    /** [conceptId, count] for every concept that has active questions. */
    @Query("select q.conceptId, count(q) from QuizQuestion q where q.active = true group by q.conceptId")
    List<Object[]> countActiveByConcept();
}
