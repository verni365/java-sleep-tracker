package ru.yandex.practicum.sleeptracker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChronotypeFunction implements SleepAnalysisFunction {
    public enum Chronotype { OWL, LARK, PIGEON }

    @Override
    public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
        if (sessions.isEmpty()) {
            return new SleepAnalysisResult("Хронотип пользователя", Chronotype.PIGEON.toString());
        }
        LocalDateTime firstStart = sessions.stream()
                .map(SleepingSession::getStart)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new IllegalStateException("Не удалось определить первую сессию сна"));
        LocalDateTime lastEnd = sessions.stream()
                .map(SleepingSession::getEnd)
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new IllegalStateException("Не удалось определить последнюю сессию сна"));
        LocalDate startDate = firstStart.toLocalDate();
        LocalDate endDate = lastEnd.toLocalDate();

        Map<LocalDate, SleepingSession> nightToSession = sessions.stream()
                .flatMap(session -> generateDatesBetween(session.getStart().toLocalDate(), session.getEnd().toLocalDate()).stream()
                        .filter(date -> session.coversNight(date.atStartOfDay()))
                        .map(date -> Map.entry(date, session)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1));

        Map<Chronotype, Long> chronotypeCount = nightToSession.entrySet().stream()
                .map(entry -> {
                    SleepingSession session = entry.getValue();
                    LocalTime bed = session.getStart().toLocalTime();
                    LocalTime wake = session.getEnd().toLocalTime();
                    if (bed.isAfter(LocalTime.of(23, 0)) && wake.isAfter(LocalTime.of(9, 0)))
                        return Chronotype.OWL;
                    else if (bed.isBefore(LocalTime.of(22, 0)) && wake.isBefore(LocalTime.of(7, 0)))
                        return Chronotype.LARK;
                    else
                        return Chronotype.PIGEON;
                })
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        Chronotype result = chronotypeCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Chronotype.PIGEON);

        long maxCount = chronotypeCount.values().stream().max(Long::compareTo).orElse(0L);
        if (chronotypeCount.values().stream().filter(v -> v == maxCount).count() > 1) {
            result = Chronotype.PIGEON;
        }
        return new SleepAnalysisResult("Хронотип пользователя", result);
    }

    private List<LocalDate> generateDatesBetween(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) return List.of();
        return Stream.iterate(start, d -> d.plusDays(1))
                .limit(java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1)
                .collect(Collectors.toList());
    }
}