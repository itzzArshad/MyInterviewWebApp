package com.academy.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    boolean existsByUserIdAndActivityDate(Long userId, LocalDate date);

    List<ActivityLog> findByUserIdAndActivityDateGreaterThanEqualOrderByActivityDateDesc(Long userId, LocalDate from);

    @Modifying
    @Query("update ActivityLog a set a.itemsCompleted = a.itemsCompleted + 1 where a.userId = :userId and a.activityDate = :date")
    int incrementItemsCompleted(@Param("userId") Long userId, @Param("date") LocalDate date);
}
