package com.academy.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    List<InterviewAnswer> findByUserIdAndCreatedAtGreaterThanEqual(Long userId, OffsetDateTime since);

    List<InterviewAnswer> findByUserId(Long userId);

    List<InterviewAnswer> findByMockSessionIdOrderByIdAsc(Long mockSessionId);

    boolean existsByUserIdAndCreatedAtGreaterThanEqual(Long userId, OffsetDateTime since);
}
