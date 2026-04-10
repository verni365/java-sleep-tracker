package ru.yandex.practicum.sleeptracker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

public class SleeplessNightsFunction implements SleepAnalysisFunction {
    @Override
    public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
        if (sessions.isEmpty()) {
            return new SleepAnalysisResult("Количество бессонных ночей", 0L);
        }

        LocalDateTime firstStart = sessions.stream()
                .map(SleepingSession::getStart)
                .min(LocalDateTime::compareTo)
                .orElseThrow();
        LocalDateTime lastEnd = sessions.stream()
                .map(SleepingSession::getEnd)
                .max(LocalDateTime::compareTo)
                .orElseThrow();

        LocalDate startDate = firstStart.toLocalDate();
        if (firstStart.getHour() >= 12) {
            startDate = startDate.plusDays(1);
        }

        LocalDate endDate = lastEnd.toLocalDate();
        boolean singleSession = sessions.size() == 1;
        if (singleSession && lastEnd.getHour() >= 6) {
            endDate = endDate.plusDays(1);
        }

        long sleeplessNights = Stream.iterate(startDate, d -> d.plusDays(1))
                .limit(ChronoUnit.DAYS.between(startDate, endDate) + 1)
                .filter(nightDate -> {
                    LocalDateTime nightStart = nightDate.atStartOfDay();
                    LocalDateTime nightEnd = nightStart.plusHours(6);
                    boolean hasSleep = sessions.stream().anyMatch(session -> {
                        LocalDateTime sStart = session.getStart();
                        LocalDateTime sEnd = session.getEnd();
                        return sStart.isBefore(nightEnd) && sEnd.isAfter(nightStart);
                    });
                    return !hasSleep;
                })
                .count();

        return new SleepAnalysisResult("Количество бессонных ночей", sleeplessNights);
    }
}