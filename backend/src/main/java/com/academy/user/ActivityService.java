package com.academy.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ActivityService {

    private final ActivityLogRepository repo;

    public ActivityService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    public void touchToday(Long userId) {
        LocalDate today = LocalDate.now();
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
        repo.incrementItemsCompleted(userId, LocalDate.now());
    }

    public List<LocalDate> activeDaysSince(Long userId, LocalDate from) {
        return repo.findByUserIdAndActivityDateGreaterThanEqualOrderByActivityDateDesc(userId, from)
                .stream().map(ActivityLog::getActivityDate).toList();
    }

    /**
     * Consecutive active days ending today - or ending yesterday, so the streak
     * is not shown as broken before you have studied today.
     */
    public static int currentStreak(List<LocalDate> activeDaysDesc, LocalDate today) {
        int streak = 0;
        LocalDate expected = activeDaysDesc.contains(today) ? today : today.minusDays(1);
        for (LocalDate d : activeDaysDesc) {
            if (d.isAfter(expected)) continue;
            if (!d.equals(expected)) break;
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }
}
