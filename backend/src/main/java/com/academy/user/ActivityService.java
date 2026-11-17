package com.academy.user;

import com.academy.common.AppTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ActivityService {

    /** A day in the activity history: opened the app, and whether the training session was finished. */
    public record Day(LocalDate date, boolean completed) {
    }

    private final ActivityLogRepository repo;
    private final AppTime time;

    public ActivityService(ActivityLogRepository repo, AppTime time) {
        this.repo = repo;
        this.time = time;
    }

    public void touchToday(Long userId) {
        LocalDate today = time.today();
        if (repo.existsByUserIdAndActivityDate(userId, today)) return;
        try {
            repo.save(new ActivityLog(userId, today));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent insert for the same day - fine
        }
    }

    /** Marks today active and counts one more completed item (concept or quiz). */
    @Transactional
    public void recordCompletion(Long userId) {
        touchToday(userId);
        repo.incrementItemsCompleted(userId, time.today());
    }

    /** Sets whether today's training session is complete (it can become un-complete only on the same day). */
    @Transactional
    public void setSessionCompleted(Long userId, boolean completed) {
        touchToday(userId);
        repo.findByUserIdAndActivityDate(userId, time.today()).ifPresent(a -> a.setSessionCompleted(completed));
    }

    public List<Day> daysSince(Long userId, LocalDate from) {
        return repo.findByUserIdAndActivityDateGreaterThanEqualOrderByActivityDateDesc(userId, from)
                .stream().map(a -> new Day(a.getActivityDate(), a.isSessionCompleted())).toList();
    }

    /** Streak = consecutive days with a completed training session (opening the app is not enough). */
    public int sessionStreak(Long userId) {
        LocalDate today = time.today();
        List<LocalDate> completed = daysSince(userId, today.minusDays(400)).stream()
                .filter(Day::completed).map(Day::date).toList();
        return currentStreak(completed, today);
    }

    /**
     * Consecutive days ending today - or ending yesterday, so the streak
     * is not shown as broken before you have studied today.
     */
    public static int currentStreak(List<LocalDate> daysDesc, LocalDate today) {
        int streak = 0;
        LocalDate expected = daysDesc.contains(today) ? today : today.minusDays(1);
        for (LocalDate d : daysDesc) {
            if (d.isAfter(expected)) continue;
            if (!d.equals(expected)) break;
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }
}
