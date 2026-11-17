package com.academy.revision;

/**
 * SM-2 spaced-repetition scheduling (the algorithm behind Anki), with Anki-style grades.
 * Pure function: state in, state out — trivially testable.
 *
 * <pre>
 * Again → relearn: interval 0, due again in ~10 minutes, ease −0.20, lapse counted
 * Hard  → first review 1 day, else interval × 1.2,          ease −0.15
 * Good  → 1 day, then 3 days, then interval × ease
 * Easy  → 4 days, then interval × ease × 1.3,              ease +0.15
 * </pre>
 * Ease never drops below 1.3; intervals are capped at one year.
 */
public final class Scheduler {

    public static final double MIN_EASE = 1.3;
    public static final int MAX_INTERVAL = 365;

    public enum Grade {
        AGAIN(1), HARD(2), GOOD(3), EASY(4);

        public final int value;

        Grade(int value) {
            this.value = value;
        }

        public static Grade of(int v) {
            for (Grade g : values()) if (g.value == v) return g;
            throw new IllegalArgumentException("Grade must be 1-4");
        }
    }

    /** @param intervalDays 0 means "relearning / not yet scheduled in days" */
    public record State(double ease, int intervalDays, int repetitions, int lapses) {
        public static State fresh() {
            return new State(2.5, 0, 0, 0);
        }
    }

    private Scheduler() {
    }

    public static State next(State s, Grade g) {
        double ease = s.ease();
        int interval;
        int reps = s.repetitions();
        int lapses = s.lapses();
        switch (g) {
            case AGAIN -> {
                ease -= 0.20;
                interval = 0;
                reps = 0;
                lapses++;
            }
            case HARD -> {
                ease -= 0.15;
                interval = reps == 0 ? 1 : (int) Math.round(Math.max(1, s.intervalDays()) * 1.2);
                reps++;
            }
            case GOOD -> {
                interval = reps == 0 ? 1 : reps == 1 ? 3 : (int) Math.round(Math.max(1, s.intervalDays()) * ease);
                reps++;
            }
            case EASY -> {
                ease += 0.15;
                interval = reps == 0 ? 4 : (int) Math.round(Math.max(1, s.intervalDays()) * ease * 1.3);
                reps++;
            }
            default -> throw new IllegalStateException();
        }
        ease = Math.max(MIN_EASE, Math.round(ease * 100) / 100.0);
        // Never schedule a passed card sooner than it already was (Hard on a long interval stays long-ish).
        if (g != Grade.AGAIN && s.intervalDays() > 0) interval = Math.max(interval, g == Grade.HARD ? s.intervalDays() : s.intervalDays() + 1);
        return new State(ease, Math.min(interval, MAX_INTERVAL), reps, lapses);
    }

    /** Estimated chance you still remember (≈ 90% exactly at the due date, decaying after). */
    public static double retrievability(double daysSinceReview, int intervalDays) {
        return Math.pow(0.9, Math.max(0, daysSinceReview) / Math.max(1, intervalDays));
    }

    /** Maps an explanation score (share of key points covered) to a review grade. */
    public static Grade gradeForScore(int scorePct) {
        if (scorePct < 50) return Grade.AGAIN;
        if (scorePct < 70) return Grade.HARD;
        if (scorePct < 90) return Grade.GOOD;
        return Grade.EASY;
    }
}
