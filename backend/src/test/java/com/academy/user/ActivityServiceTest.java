package com.academy.user;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityServiceTest {

    private final LocalDate today = LocalDate.of(2026, 11, 15);

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        var days = List.of(today, today.minusDays(1), today.minusDays(2), today.minusDays(4));
        assertThat(ActivityService.currentStreak(days, today)).isEqualTo(3);
    }

    @Test
    void streakStillAliveIfLastActiveYesterday() {
        var days = List.of(today.minusDays(1), today.minusDays(2));
        assertThat(ActivityService.currentStreak(days, today)).isEqualTo(2);
    }

    @Test
    void streakBrokenAfterMissedDay() {
        var days = List.of(today.minusDays(2), today.minusDays(3));
        assertThat(ActivityService.currentStreak(days, today)).isZero();
    }

    @Test
    void emptyHistoryIsZero() {
        assertThat(ActivityService.currentStreak(List.of(), today)).isZero();
    }
}
