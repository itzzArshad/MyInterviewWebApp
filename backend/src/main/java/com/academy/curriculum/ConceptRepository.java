package com.academy.curriculum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    Optional<Concept> findBySlug(String slug);

    /** Projection query: builds the navigation tree without loading the (large) lesson bodies. */
    @Query("""
            select new com.academy.curriculum.ConceptSummary(
                c.id, c.chapterId, c.slug, c.title, c.difficulty, c.status, c.estMinutes, c.sortOrder)
            from Concept c
            where c.status <> :excluded
            order by c.sortOrder""")
    List<ConceptSummary> findSummariesExcluding(@Param("excluded") Concept.Status excluded);

    default List<ConceptSummary> findAllSummaries() {
        return findSummariesExcluding(Concept.Status.ARCHIVED);
    }

    List<Concept> findAllByStatusNot(Concept.Status status);
}
