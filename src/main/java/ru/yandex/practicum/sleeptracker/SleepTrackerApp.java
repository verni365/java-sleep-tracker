package ru.yandex.practicum.sleeptracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class SleepTrackerApp {
    private static final List<SleepAnalysisFunction> ANALYSIS_FUNCTIONS = List.of(
            new TotalSessionsFunction(),
            new MinDurationFunction(),
            new MaxDurationFunction(),
            new AvgDurationFunction(),
            new BadQualitySessionsFunction(),
            new SleeplessNightsFunction(),
            new ChronotypeFunction()
    );

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Укажите путь к файлу с логом сна.");
            return;
        }
        String filePath = args[0];
        try {
            List<SleepingSession> sessions = loadSessions(filePath);
            ANALYSIS_FUNCTIONS.stream()
                    .map(func -> func.analyze(sessions))
                    .forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    private static List<SleepingSession> loadSessions(String filePath) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
        return Files.lines(Path.of(filePath))
                .map(line -> {
                    String[] parts = line.split(";");
                    if (parts.length != 3) throw new RuntimeException("Неверный формат: " + line);
                    LocalDateTime start = LocalDateTime.parse(parts[0], formatter);
                    LocalDateTime end = LocalDateTime.parse(parts[1], formatter);
                    SleepingSession.Quality quality = SleepingSession.Quality.valueOf(parts[2]);
                    return new SleepingSession(start, end, quality);
                })
                .collect(Collectors.toList());
    }
}