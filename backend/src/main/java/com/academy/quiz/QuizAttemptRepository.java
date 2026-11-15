package com.academy.quiz;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndConceptIdAndFinishedAtIsNotNull(Long userId, Long conceptId);

    Optional<QuizAttempt> findFirstByUserIdAndConceptIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(Long userId, Long conceptId);

    List<QuizAttempt> findByUserIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(Long userId, Limit limit);

    @Query("select distinct a.conceptId from QuizAttempt a where a.userId = :userId and a.finishedAt is not null")
    List<Long> findQuizzedConceptIds(@Param("userId") Long userId);
}
