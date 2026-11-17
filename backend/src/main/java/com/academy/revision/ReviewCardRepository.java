package com.academy.revision;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewCardRepository extends JpaRepository<ReviewCard, Long> {

    Optional<ReviewCard> findByUserIdAndSourceKey(Long userId, String sourceKey);

    Optional<ReviewCard> findByIdAndUserId(Long id, Long userId);

    List<ReviewCard> findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(Long userId, OffsetDateTime until, Limit limit);

    long countByUserIdAndDueAtLessThanEqual(Long userId, OffsetDateTime until);

    long countByUserId(Long userId);

    List<ReviewCard> findByUserId(Long userId);
}
