package com.academy.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterviewItemRepository extends JpaRepository<InterviewItem, Long> {

    List<InterviewItem> findByConceptId(Long conceptId);

    List<InterviewItem> findByActiveTrue();

    long countByConceptIdAndActiveTrue(Long conceptId);

    @Query("select distinct i.conceptId from InterviewItem i")
    List<Long> findConceptIdsWithItems();
}
