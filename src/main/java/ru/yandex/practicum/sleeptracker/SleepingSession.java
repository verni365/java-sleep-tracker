package ru.yandex.practicum.sleeptracker;

import java.time.Duration;
import java.time.LocalDateTime;

public class SleepingSession {
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final SleepQuality quality;

    public SleepingSession(LocalDateTime start, LocalDateTime end, SleepQuality quality) {
        if (start == null || end == null || quality == null) {
            throw new IllegalArgumentException("Все параметры обязательны");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Время пробуждения не может быть раньше времени засыпания");
        }
        this.start = start;
        this.end = end;
        this.quality = quality;
    }

    public LocalDateTime getStartDateTime() {
        return start;
    }

    public LocalDateTime getEndDateTime() {
        return end;
    }

    public SleepQuality getQuality() {
        return quality;
    }

    public long getDurationMinutes() {
        return Duration.between(start, end).toMinutes();
    }
}