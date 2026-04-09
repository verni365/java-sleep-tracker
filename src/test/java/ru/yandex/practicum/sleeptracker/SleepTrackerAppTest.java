package ru.yandex.practicum.sleeptracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SleepTrackerAppTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private List<SleepingSession> sessions;

    @BeforeEach
    void setUp() {
        sessions = List.of(
                session("01.10.25 23:15", "02.10.25 07:30", SleepQuality.GOOD),
                session("02.10.25 23:50", "03.10.25 06:40", SleepQuality.NORMAL),
                session("03.10.25 14:10", "03.10.25 15:00", SleepQuality.NORMAL),
                session("03.10.25 23:40", "04.10.25 08:00", SleepQuality.BAD),
                session("05.10.25 00:10", "05.10.25 06:20", SleepQuality.GOOD),
                session("05.10.25 13:30", "05.10.25 14:15", SleepQuality.NORMAL),
                session("06.10.25 22:30", "07.10.25 05:50", SleepQuality.GOOD),
                session("07.10.25 23:45", "08.10.25 06:30", SleepQuality.GOOD),
                session("08.10.25 23:50", "09.10.25 07:10", SleepQuality.GOOD),
                session("10.10.25 13:00", "10.10.25 14:30", SleepQuality.NORMAL),
                session("10.10.25 23:55", "11.10.25 06:10", SleepQuality.GOOD),
                session("11.10.25 23:10", "12.10.25 07:00", SleepQuality.BAD),
                session("30.10.25 23:50", "31.10.25 06:30", SleepQuality.GOOD)
        );
    }

    private SleepingSession session(String start, String end, SleepQuality quality) {
        return new SleepingSession(
                LocalDateTime.parse(start, FMT),
                LocalDateTime.parse(end, FMT),
                quality
        );
    }

    // ----- Total sessions -----
    @Test
    void testTotalSessions() {
        var result = new SleepTrackerApp.TotalSessionsAnalyzer().analyze(sessions);
        assertEquals(13L, result.getValue());
    }

    @Test
    void testTotalSessionsEmpty() {
        var result = new SleepTrackerApp.TotalSessionsAnalyzer().analyze(List.of());
        assertEquals(0L, result.getValue());
    }

    // ----- Min duration -----
    @Test
    void testMinDuration() {
        var result = new SleepTrackerApp.MinDurationAnalyzer().analyze(sessions);
        assertEquals("0 ч 50 мин", result.getValue()); // 50 минут
    }

    @Test
    void testMinDurationEmpty() {
        var result = new SleepTrackerApp.MinDurationAnalyzer().analyze(List.of());
        assertEquals("0 ч 0 мин", result.getValue());
    }

    // ----- Max duration -----
    @Test
    void testMaxDuration() {
        var result = new SleepTrackerApp.MaxDurationAnalyzer().analyze(sessions);
        assertEquals("8 ч 15 мин", result.getValue()); // 495 минут = 8ч15м
    }

    @Test
    void testMaxDurationEmpty() {
        var result = new SleepTrackerApp.MaxDurationAnalyzer().analyze(List.of());
        assertEquals("0 ч 0 мин", result.getValue());
    }

    // ----- Average duration -----
    @Test
    void testAvgDuration() {
        var result = new SleepTrackerApp.AverageDurationAnalyzer().analyze(sessions);
        // Примерное среднее: 344 минуты ≈ 5ч44м, но точное значение может немного отличаться
        String avg = (String) result.getValue();
        assertTrue(avg.contains("ч") && avg.contains("мин"));
    }

    @Test
    void testAvgDurationEmpty() {
        var result = new SleepTrackerApp.AverageDurationAnalyzer().analyze(List.of());
        assertEquals("0 ч 0 мин", result.getValue());
    }

    // ----- Bad quality sessions -----
    @Test
    void testBadQualityCount() {
        var result = new SleepTrackerApp.BadQualitySessionsAnalyzer().analyze(sessions);
        assertEquals(2L, result.getValue()); // две сессии с BAD
    }

    @Test
    void testBadQualityEmpty() {
        var result = new SleepTrackerApp.BadQualitySessionsAnalyzer().analyze(List.of());
        assertEquals(0L, result.getValue());
    }

    // ----- Sleepless nights (4+ тестов) -----
    @Test
    void testSleeplessNightsNormalData() {
        var result = new SleepTrackerApp.SleeplessNightsAnalyzer().analyze(sessions);
        // В наших данных все ночи покрыты – бессонных нет
        assertEquals(0L, result.getValue());
    }

    @Test
    void testSleeplessNightsOneDaytimeOnly() {
        List<SleepingSession> data = List.of(
                session("01.10.25 07:00", "01.10.25 12:00", SleepQuality.GOOD)
        );
        var result = new SleepTrackerApp.SleeplessNightsAnalyzer().analyze(data);
        // Первая сессия началась в 7:00 (до 12) -> первая ночь = 01.10.
        // Сессия не покрывает ночь 01.10 (с 7:00 до 12:00 не пересекает [0:00,6:00)).
        // Всего ночей с 01.10 по 01.10 = 1, покрытых 0 -> 1 бессонная.
        assertEquals(1L, result.getValue());
    }

    @Test
    void testSleeplessNightsNightSession() {
        List<SleepingSession> data = List.of(
                session("01.10.25 23:00", "02.10.25 07:00", SleepQuality.GOOD)
        );
        var result = new SleepTrackerApp.SleeplessNightsAnalyzer().analyze(data);
        // Первая сессия началась в 23:00 (<12) -> первая ночь = 01.10.
        // Сессия покрывает ночь 02.10 (с 23:00 до 7:00 пересекает 02.10 00:00-06:00).
        // Ночь 01.10 не покрыта. Всего ночей с 01.10 по 02.10 = 2, покрыта 1 -> 1 бессонная.
        assertEquals(1L, result.getValue());
    }

    @Test
    void testSleeplessNightsAfterNoonStart() {
        List<SleepingSession> data = List.of(
                session("01.10.25 13:00", "01.10.25 15:00", SleepQuality.GOOD)
        );
        var result = new SleepTrackerApp.SleeplessNightsAnalyzer().analyze(data);
        // Начало после 12 -> первая ночь = 02.10. Конец 01.10 -> lastNight = 01.10.
        // firstNight > lastNight -> ночей нет, бессонных 0.
        assertEquals(0L, result.getValue());
    }

    @Test
    void testSleeplessNightsEmpty() {
        var result = new SleepTrackerApp.SleeplessNightsAnalyzer().analyze(List.of());
        assertEquals(0L, result.getValue());
    }

    // ----- Chronotype -----
    @Test
    void testChronotypeOwl() {
        List<SleepingSession> owlData = List.of(
                session("01.10.25 23:30", "02.10.25 09:30", SleepQuality.GOOD),
                session("02.10.25 23:45", "03.10.25 10:00", SleepQuality.GOOD)
        );
        var result = new SleepTrackerApp.ChronotypeAnalyzer().analyze(owlData);
        assertEquals("Сова", result.getValue());
    }

    @Test
    void testChronotypeLark() {
        List<SleepingSession> larkData = List.of(
                session("01.10.25 21:30", "02.10.25 06:30", SleepQuality.GOOD),
                session("02.10.25 21:45", "03.10.25 06:45", SleepQuality.GOOD)
        );
        var result = new SleepTrackerApp.ChronotypeAnalyzer().analyze(larkData);
        assertEquals("Жаворонок", result.getValue());
    }

    @Test
    void testChronotypePigeonWhenMixed() {
        List<SleepingSession> mixed = List.of(
                session("01.10.25 23:30", "02.10.25 09:30", SleepQuality.GOOD), // сова
                session("02.10.25 21:30", "03.10.25 06:30", SleepQuality.GOOD)  // жаворонок
        );
        var result = new SleepTrackerApp.ChronotypeAnalyzer().analyze(mixed);
        assertEquals("Голубь", result.getValue());
    }

    @Test
    void testChronotypeIgnoreDaytime() {
        List<SleepingSession> withDaytime = List.of(
                session("01.10.25 23:30", "02.10.25 09:30", SleepQuality.GOOD), // сова
                session("02.10.25 14:00", "02.10.25 15:00", SleepQuality.GOOD)   // дневная
        );
        var result = new SleepTrackerApp.ChronotypeAnalyzer().analyze(withDaytime);
        assertEquals("Сова", result.getValue()); // дневная игнорируется
    }
}