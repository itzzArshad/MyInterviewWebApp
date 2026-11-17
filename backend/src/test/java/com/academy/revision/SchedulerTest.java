package com.academy.revision;

import com.academy.revision.Scheduler.Grade;
import com.academy.revision.Scheduler.State;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerTest {

    @Test
    void goodProgressionIsOneThreeThenGrowsByEase() {
        State s = State.fresh();
        s = Scheduler.next(s, Grade.GOOD);
        assertThat(s.intervalDays()).isEqualTo(1);
        s = Scheduler.next(s, Grade.GOOD);
        assertThat(s.intervalDays()).isEqualTo(3);
        s = Scheduler.next(s, Grade.GOOD);
        assertThat(s.intervalDays()).isEqualTo(8);   // 3 × 2.5 rounded
        s = Scheduler.next(s, Grade.GOOD);
        assertThat(s.intervalDays()).isEqualTo(20);
        assertThat(s.repetitions()).isEqualTo(4);
    }

    @Test
    void againResetsAndCountsALapseAndLowersEase() {
        State s = new State(2.5, 20, 4, 0);
        State after = Scheduler.next(s, Grade.AGAIN);
        assertThat(after.intervalDays()).isZero();
        assertThat(after.repetitions()).isZero();
        assertThat(after.lapses()).isEqualTo(1);
        assertThat(after.ease()).isEqualTo(2.3);
    }

    @Test
    void easeNeverDropsBelowMinimum() {
        State s = State.fresh();
        for (int i = 0; i < 20; i++) s = Scheduler.next(s, Grade.AGAIN);
        assertThat(s.ease()).isEqualTo(Scheduler.MIN_EASE);
    }

    @Test
    void easyBeatsGoodAndHardNeverShrinksAnInterval() {
        State s = new State(2.5, 10, 3, 0);
        assertThat(Scheduler.next(s, Grade.EASY).intervalDays()).isGreaterThan(Scheduler.next(s, Grade.GOOD).intervalDays());
        assertThat(Scheduler.next(s, Grade.HARD).intervalDays()).isGreaterThanOrEqualTo(10);
        assertThat(Scheduler.next(new State(2.5, 300, 9, 0), Grade.EASY).intervalDays()).isEqualTo(Scheduler.MAX_INTERVAL);
    }

    @Test
    void retrievabilityIsNinetyPercentAtDueDateAndDecays() {
        assertThat(Scheduler.retrievability(0, 10)).isEqualTo(1.0);
        assertThat(Scheduler.retrievability(10, 10)).isCloseTo(0.9, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(Scheduler.retrievability(30, 10)).isLessThan(0.75);
    }

    @Test
    void explainScoreMapsToGrade() {
        assertThat(Scheduler.gradeForScore(40)).isEqualTo(Grade.AGAIN);
        assertThat(Scheduler.gradeForScore(60)).isEqualTo(Grade.HARD);
        assertThat(Scheduler.gradeForScore(80)).isEqualTo(Grade.GOOD);
        assertThat(Scheduler.gradeForScore(100)).isEqualTo(Grade.EASY);
    }
}
