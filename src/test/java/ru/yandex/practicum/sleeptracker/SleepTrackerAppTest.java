package ru.yandex.practicum.sleeptracker;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SleepTrackerAppTest {

    private SleepingSession session(int dayStart, int hourStart, int minuteStart,
                                    int dayEnd, int hourEnd, int minuteEnd,
                                    SleepingSession.Quality quality) {
        return new SleepingSession(
                LocalDateTime.of(2025, Month.OCTOBER, dayStart, hourStart, minuteStart),
                LocalDateTime.of(2025, Month.OCTOBER, dayEnd, hourEnd, minuteEnd),
                quality
        );
    }

    @Test
    void testTotalSessionsEmpty() {
        TotalSessionsFunction func = new TotalSessionsFunction();
        assertEquals(0L, func.analyze(List.of()).getValue());
    }

    @Test
    void testTotalSessionsMultiple() {
        TotalSessionsFunction func = new TotalSessionsFunction();
        var sessions = List.of(
                session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD),
                session(2, 23, 0, 3, 7, 0, SleepingSession.Quality.NORMAL)
        );
        assertEquals(2L, func.analyze(sessions).getValue());
    }

    @Test
    void testMinDuration() {
        MinDurationFunction func = new MinDurationFunction();
        var sessions = List.of(
                session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD), // 600 мин
                session(2, 23, 0, 3, 7, 0, SleepingSession.Quality.NORMAL)  // 480 мин
        );
        assertEquals(480L, func.analyze(sessions).getValue());
    }

    @Test
    void testMinDurationSingle() {
        MinDurationFunction func = new MinDurationFunction();
        var sessions = List.of(session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD));
        assertEquals(600L, func.analyze(sessions).getValue());
    }

    @Test
    void testMaxDuration() {
        MaxDurationFunction func = new MaxDurationFunction();
        var sessions = List.of(
                session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD),
                session(2, 23, 0, 3, 7, 0, SleepingSession.Quality.NORMAL)
        );
        assertEquals(600L, func.analyze(sessions).getValue());
    }

    @Test
    void testMaxDurationSingle() {
        MaxDurationFunction func = new MaxDurationFunction();
        var sessions = List.of(session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD));
        assertEquals(600L, func.analyze(sessions).getValue());
    }

    @Test
    void testAvgDuration() {
        AvgDurationFunction func = new AvgDurationFunction();
        var sessions = List.of(
                session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD),
                session(2, 23, 0, 3, 7, 0, SleepingSession.Quality.NORMAL)
        );
        double result = (Double) func.analyze(sessions).getValue();
        assertEquals(540.0, result, 0.001);
    }

    @Test
    void testAvgDurationEmpty() {
        AvgDurationFunction func = new AvgDurationFunction();
        double result = (Double) func.analyze(List.of()).getValue();
        assertEquals(0.0, result, 0.001);
    }

    @Test
    void testBadQualityCount() {
        BadQualitySessionsFunction func = new BadQualitySessionsFunction();
        var sessions = List.of(
                session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.BAD),
                session(2, 23, 0, 3, 7, 0, SleepingSession.Quality.NORMAL),
                session(3, 0, 0, 3, 6, 0, SleepingSession.Quality.BAD)
        );
        assertEquals(2L, func.analyze(sessions).getValue());
    }

    @Test
    void testBadQualityNone() {
        BadQualitySessionsFunction func = new BadQualitySessionsFunction();
        var sessions = List.of(
                session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD),
                session(2, 23, 0, 3, 7, 0, SleepingSession.Quality.NORMAL)
        );
        assertEquals(0L, func.analyze(sessions).getValue());
    }

    @Test
    void testSleeplessNightsNone() {
        SleeplessNightsFunction func = new SleeplessNightsFunction();
        var sessions = List.of(
                session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD),
                session(2, 23, 0, 3, 7, 0, SleepingSession.Quality.NORMAL)
        );
        assertEquals(0L, func.analyze(sessions).getValue());
    }

    @Test
    void testSleeplessNightsOneSleepless() {
        SleeplessNightsFunction func = new SleeplessNightsFunction();
        var sessions = List.of(session(1, 22, 0, 2, 8, 0, SleepingSession.Quality.GOOD));
        assertEquals(1L, func.analyze(sessions).getValue());
    }

    @Test
    void testSleeplessNightsAllSleepless() {
        SleeplessNightsFunction func = new SleeplessNightsFunction();
        var sessions = List.<SleepingSession>of();
        assertEquals(0L, func.analyze(sessions).getValue());
    }

    @Test
    void testSleeplessNightsWithDaySessionOnly() {
        SleeplessNightsFunction func = new SleeplessNightsFunction();
        var sessions = List.of(session(3, 14, 30, 3, 15, 20, SleepingSession.Quality.NORMAL));
        assertEquals(1L, func.analyze(sessions).getValue());
    }

    @Test
    void testChronotypeOwl() {
        ChronotypeFunction func = new ChronotypeFunction();
        var sessions = List.of(session(1, 23, 30, 2, 9, 30, SleepingSession.Quality.GOOD));
        ChronotypeFunction.Chronotype result = (ChronotypeFunction.Chronotype) func.analyze(sessions).getValue();
        assertEquals(ChronotypeFunction.Chronotype.OWL, result);
    }

    @Test
    void testChronotypeLark() {
        ChronotypeFunction func = new ChronotypeFunction();
        var sessions = List.of(session(1, 21, 0, 2, 6, 30, SleepingSession.Quality.GOOD));
        ChronotypeFunction.Chronotype result = (ChronotypeFunction.Chronotype) func.analyze(sessions).getValue();
        assertEquals(ChronotypeFunction.Chronotype.LARK, result);
    }

    @Test
    void testChronotypePigeon() {
        ChronotypeFunction func = new ChronotypeFunction();
        var sessions = List.of(session(1, 22, 30, 2, 8, 0, SleepingSession.Quality.GOOD));
        ChronotypeFunction.Chronotype result = (ChronotypeFunction.Chronotype) func.analyze(sessions).getValue();
        assertEquals(ChronotypeFunction.Chronotype.PIGEON, result);
    }

    @Test
    void testChronotypeTie() {
        ChronotypeFunction func = new ChronotypeFunction();
        var sessions = List.of(
                session(1, 23, 30, 2, 9, 30, SleepingSession.Quality.GOOD), // сова
                session(2, 21, 0, 3, 6, 30, SleepingSession.Quality.GOOD)    // жаворонок
        );
        ChronotypeFunction.Chronotype result = (ChronotypeFunction.Chronotype) func.analyze(sessions).getValue();
        assertEquals(ChronotypeFunction.Chronotype.PIGEON, result); // ничья -> голубь
    }
}