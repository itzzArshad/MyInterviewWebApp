package com.academy.revision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MistakeRepository extends JpaRepository<Mistake, Long> {

    Optional<Mistake> findByUserIdAndQuestionId(Long userId, Long questionId);

    Optional<Mistake> findByIdAndUserId(Long id, Long userId);

    List<Mistake> findByUserIdOrderByResolvedAscLastWrongAtDesc(Long userId);

    long countByUserIdAndResolvedFalse(Long userId);
}
