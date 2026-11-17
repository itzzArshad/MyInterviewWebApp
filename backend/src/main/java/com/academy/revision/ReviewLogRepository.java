package com.academy.revision;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    long countByUserIdAndReviewedAtGreaterThanEqual(Long userId, OffsetDateTime since);
}
