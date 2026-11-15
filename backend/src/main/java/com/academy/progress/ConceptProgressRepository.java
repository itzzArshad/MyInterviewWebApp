package com.academy.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConceptProgressRepository extends JpaRepository<ConceptProgress, ConceptProgress.Key> {

    List<ConceptProgress> findByUserId(Long userId);

    Optional<ConceptProgress> findByUserIdAndConceptId(Long userId, Long conceptId);
}
