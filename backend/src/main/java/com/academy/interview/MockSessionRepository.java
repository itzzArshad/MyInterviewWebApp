package com.academy.interview;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockSessionRepository extends JpaRepository<MockSession, Long> {

    Optional<MockSession> findByIdAndUserId(Long id, Long userId);

    List<MockSession> findByUserIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(Long userId, Limit limit);
}
