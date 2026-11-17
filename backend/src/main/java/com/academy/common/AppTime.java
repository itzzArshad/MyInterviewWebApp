package com.academy.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * The learner's calendar. Streaks, "due today" and daily sessions follow the user's local day
 * (servers such as Render run on UTC, which would split an Indian evening across two days).
 */
@Component
public class AppTime {

    private final ZoneId zone;

    public AppTime(@Value("${academy.timezone:Asia/Kolkata}") String timezone) {
        this.zone = ZoneId.of(timezone);
    }

    public ZoneId zone() {
        return zone;
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(zone);
    }

    public OffsetDateTime startOf(LocalDate day) {
        return day.atStartOfDay(zone).toOffsetDateTime();
    }

    public LocalDate dayOf(OffsetDateTime t) {
        return t.atZoneSameInstant(zone).toLocalDate();
    }
}
