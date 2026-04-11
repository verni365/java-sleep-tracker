package ru.yandex.practicum.sleeptracker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class SleepingSession {
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final Quality quality;

    public enum Quality {
        GOOD, NORMAL, BAD
    }

    public SleepingSession(LocalDateTime start, LocalDateTime end, Quality quality) {
        this.start = start;
        this.end = end;
        this.quality = quality;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public Quality getQuality() {
        return quality;
    }

    public long getDurationMinutes() {
        return ChronoUnit.MINUTES.between(start, end);
    }

    public boolean coversNight(LocalDateTime nightDate) {
        LocalDateTime nightStart = nightDate.withHour(0).withMinute(0);
        LocalDateTime nightEnd = nightDate.withHour(6).withMinute(0);
        return start.isBefore(nightEnd) && end.isAfter(nightStart);
    }
}