package com.academy.curriculum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PathRepository extends JpaRepository<LearningPath, Long> {
    Optional<LearningPath> findBySlug(String slug);

    List<LearningPath> findAllByOrderBySortOrder();
}
