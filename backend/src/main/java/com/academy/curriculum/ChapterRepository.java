package com.academy.curriculum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    Optional<Chapter> findBySubjectIdAndSlug(Long subjectId, String slug);

    List<Chapter> findAllByOrderBySortOrder();
}
