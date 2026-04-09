package ru.yandex.practicum.sleeptracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class SleepTrackerApp {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private static final LocalTime NIGHT_START = LocalTime.of(0, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);
    private static final LocalTime NOON = LocalTime.of(12, 0);

    private final List<SleepAnalyzer> analyzers;

    public SleepTrackerApp() {
        this.analyzers = List.of(
                new TotalSessionsAnalyzer(),
                new MinDurationAnalyzer(),
                new MaxDurationAnalyzer(),
                new AverageDurationAnalyzer(),
                new BadQualitySessionsAnalyzer(),
                new SleeplessNightsAnalyzer(),
                new ChronotypeAnalyzer(),
                new QualityDistributionAnalyzer(),
                new DaytimeSessionsAnalyzer(),
                new ShortSleepAnalyzer()
        );
    }

    public List<SleepAnalysisResult> runAnalysis(List<SleepingSession> sessions) {
        return analyzers.stream()
                .map(analyzer -> analyzer.analyze(sessions))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Ошибка: не указан путь к файлу с логом сна");
            System.err.println("Использование: java SleepTrackerApp <путь_к_файлу>");
            System.exit(1);
        }
        String filePath = args[0];
        try {
            List<SleepingSession> sessions = loadSessionsFromFile(filePath);
            if (sessions.isEmpty()) {
                System.out.println("Файл не содержит данных о сессиях сна");
                return;
            }
            SleepTrackerApp app = new SleepTrackerApp();
            List<SleepAnalysisResult> results = app.runAnalysis(sessions);
            System.out.println("Результаты анализа сна");
            System.out.println("Всего обработано сессий: " + sessions.size());
            System.out.println();
            results.forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка формата данных: " + e.getMessage());
            System.exit(1);
        }
    }

    private static List<SleepingSession> loadSessionsFromFile(String filePath) throws IOException {
        try (Stream<String> lines = Files.lines(Path.of(filePath))) {
            return lines
                    .filter(line -> line != null && !line.isBlank())
                    .map(SleepTrackerApp::parseSession)
                    .collect(Collectors.toList());
        }
    }

    private static SleepingSession parseSession(String line) {
        String[] parts = line.split(";");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Некорректный формат строки: " + line +
                            ". Ожидается: дата_засыпания;дата_пробуждения;качество"
            );
        }
        try {
            LocalDateTime start = LocalDateTime.parse(parts[0].trim(), DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(parts[1].trim(), DATE_FORMATTER);
            SleepQuality quality = SleepQuality.valueOf(parts[2].trim());
            return new SleepingSession(start, end, quality);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ошибка парсинга даты в строке: " + line, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Неизвестное значение качества сна в строке: " + line +
                            ". Допустимые значения: GOOD, NORMAL, BAD", e
            );
        }
    }

    // ----- Анализаторы -----

    static class TotalSessionsAnalyzer implements SleepAnalyzer {
        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            return new SleepAnalysisResult("Общее количество сессий сна", (long) sessions.size());
        }
    }

    static class MinDurationAnalyzer implements SleepAnalyzer {
        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            long minMinutes = sessions.stream()
                    .mapToLong(SleepingSession::getDurationMinutes)
                    .min()
                    .orElse(0);
            return new SleepAnalysisResult("Минимальная продолжительность сессии сна",
                    formatDuration(minMinutes));
        }
    }

    static class MaxDurationAnalyzer implements SleepAnalyzer {
        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            long maxMinutes = sessions.stream()
                    .mapToLong(SleepingSession::getDurationMinutes)
                    .max()
                    .orElse(0);
            return new SleepAnalysisResult("Максимальная продолжительность сессии сна",
                    formatDuration(maxMinutes));
        }
    }

    static class AverageDurationAnalyzer implements SleepAnalyzer {
        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            double avgMinutes = sessions.stream()
                    .mapToLong(SleepingSession::getDurationMinutes)
                    .average()
                    .orElse(0.0);
            return new SleepAnalysisResult("Средняя продолжительность сна",
                    formatDuration((long) avgMinutes));
        }
    }

    static class BadQualitySessionsAnalyzer implements SleepAnalyzer {
        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            long count = sessions.stream()
                    .filter(s -> s.getQuality() == SleepQuality.BAD)
                    .count();
            return new SleepAnalysisResult("Количество сессий с плохим качеством сна (BAD)", count);
        }
    }

    static class SleeplessNightsAnalyzer implements SleepAnalyzer {
        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            if (sessions.isEmpty()) {
                return new SleepAnalysisResult("Количество бессонных ночей", 0L);
            }

            SleepingSession first = sessions.get(0);
            SleepingSession last = sessions.get(sessions.size() - 1);

            LocalDate firstNight = first.getStartDateTime().getHour() >= 12
                    ? first.getStartDateTime().toLocalDate().plusDays(1)
                    : first.getStartDateTime().toLocalDate();
            LocalDate lastNight = last.getEndDateTime().toLocalDate();

            long totalNights = ChronoUnit.DAYS.between(firstNight, lastNight) + 1;

            Set<LocalDate> coveredNights = sessions.stream()
                    .flatMap(session -> getNightsCoveredBySession(session).stream())
                    .collect(Collectors.toSet());

            long sleepless = totalNights - coveredNights.size();
            return new SleepAnalysisResult("Количество бессонных ночей", sleepless);
        }

        private List<LocalDate> getNightsCoveredBySession(SleepingSession session) {
            LocalDateTime start = session.getStartDateTime();
            LocalDateTime end = session.getEndDateTime();

            LocalDate firstPotentialNight = start.toLocalDate();
            if (start.toLocalTime().isAfter(NIGHT_END)) {
                firstPotentialNight = firstPotentialNight.plusDays(1);
            }

            LocalDate lastPotentialNight = end.toLocalDate();
            if (end.toLocalTime().isBefore(NIGHT_START)) {
                lastPotentialNight = lastPotentialNight.minusDays(1);
            }

            if (lastPotentialNight.isBefore(firstPotentialNight)) {
                return List.of();
            }

            long daysBetween = ChronoUnit.DAYS.between(firstPotentialNight, lastPotentialNight);
            return LongStream.rangeClosed(0, daysBetween)
                    .mapToObj(firstPotentialNight::plusDays)
                    .collect(Collectors.toList());
        }
    }

    static class ChronotypeAnalyzer implements SleepAnalyzer {
        private static final LocalTime OWL_SLEEP_START = LocalTime.of(23, 0);
        private static final LocalTime OWL_WAKE_END = LocalTime.of(9, 0);
        private static final LocalTime LARK_SLEEP_END = LocalTime.of(22, 0);
        private static final LocalTime LARK_WAKE_END = LocalTime.of(7, 0);

        enum Chronotype {
            OWL("Сова"),
            LARK("Жаворонок"),
            PIGEON("Голубь");

            private final String displayName;

            Chronotype(String displayName) {
                this.displayName = displayName;
            }

            String getDisplayName() {
                return displayName;
            }
        }

        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            if (sessions.isEmpty()) {
                return new SleepAnalysisResult("Хронотип пользователя", Chronotype.PIGEON.getDisplayName());
            }

            Set<LocalDate> coveredNights = sessions.stream()
                    .flatMap(s -> getNightsCoveredBySession(s).stream())
                    .collect(Collectors.toSet());

            Map<Chronotype, Long> typeCount = coveredNights.stream()
                    .map(night -> classifyNight(sessions, night))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

            if (typeCount.isEmpty()) {
                return new SleepAnalysisResult("Хронотип пользователя", Chronotype.PIGEON.getDisplayName());
            }

            long max = typeCount.values().stream().max(Long::compare).orElse(0L);
            boolean tie = typeCount.values().stream().filter(c -> c == max).count() > 1;
            Chronotype result = tie ? Chronotype.PIGEON
                    : typeCount.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();

            return new SleepAnalysisResult("Хронотип пользователя", result.getDisplayName());
        }

        private Optional<Chronotype> classifyNight(List<SleepingSession> sessions, LocalDate night) {
            return sessions.stream()
                    .filter(session -> isNightCovered(session, night))
                    .findFirst()
                    .map(this::classifySession);
        }

        private boolean isNightCovered(SleepingSession session, LocalDate night) {
            LocalDateTime nightStart = night.atTime(NIGHT_START);
            LocalDateTime nightEnd = night.atTime(NIGHT_END);
            return session.getStartDateTime().isBefore(nightEnd)
                    && session.getEndDateTime().isAfter(nightStart);
        }

        private Chronotype classifySession(SleepingSession session) {
            LocalTime sleepTime = session.getStartDateTime().toLocalTime();
            LocalTime wakeTime = session.getEndDateTime().toLocalTime();

            if (sleepTime.isAfter(OWL_SLEEP_START) && wakeTime.isAfter(OWL_WAKE_END)) {
                return Chronotype.OWL;
            }
            if (sleepTime.isBefore(LARK_SLEEP_END) && wakeTime.isBefore(LARK_WAKE_END)) {
                return Chronotype.LARK;
            }
            return Chronotype.PIGEON;
        }

        private List<LocalDate> getNightsCoveredBySession(SleepingSession session) {
            LocalDateTime start = session.getStartDateTime();
            LocalDateTime end = session.getEndDateTime();

            LocalDate firstNight = start.toLocalDate();
            if (start.toLocalTime().isAfter(NIGHT_END)) {
                firstNight = firstNight.plusDays(1);
            }
            LocalDate lastNight = end.toLocalDate();
            if (end.toLocalTime().isBefore(NIGHT_START)) {
                lastNight = lastNight.minusDays(1);
            }
            if (lastNight.isBefore(firstNight)) {
                return List.of();
            }
            long days = ChronoUnit.DAYS.between(firstNight, lastNight);
            return LongStream.rangeClosed(0, days)
                    .mapToObj(firstNight::plusDays)
                    .collect(Collectors.toList());
        }
    }

    static class QualityDistributionAnalyzer implements SleepAnalyzer {
        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            long good = sessions.stream().filter(s -> s.getQuality() == SleepQuality.GOOD).count();
            long normal = sessions.stream().filter(s -> s.getQuality() == SleepQuality.NORMAL).count();
            long bad = sessions.stream().filter(s -> s.getQuality() == SleepQuality.BAD).count();
            return new SleepAnalysisResult("Распределение по качеству сна",
                    String.format("GOOD: %d, NORMAL: %d, BAD: %d", good, normal, bad));
        }
    }

    static class DaytimeSessionsAnalyzer implements SleepAnalyzer {
        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            long count = sessions.stream()
                    .filter(s -> {
                        int hour = s.getStartDateTime().getHour();
                        return hour >= 6 && hour <= 20;
                    })
                    .count();
            return new SleepAnalysisResult("Количество дневных сессий сна", count);
        }
    }

    static class ShortSleepAnalyzer implements SleepAnalyzer {
        private static final long MIN_RECOMMENDED_MINUTES = 7 * 60;

        @Override
        public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
            long count = sessions.stream()
                    .filter(s -> s.getDurationMinutes() < MIN_RECOMMENDED_MINUTES)
                    .count();
            return new SleepAnalysisResult("Количество сессий с недостаточным сном (<7 ч)", count);
        }
    }

    private static String formatDuration(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        return hours + " ч " + mins + " мин";
    }
}
