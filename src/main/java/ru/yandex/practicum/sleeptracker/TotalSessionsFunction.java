package ru.yandex.practicum.sleeptracker;

import java.util.List;

public class TotalSessionsFunction implements SleepAnalysisFunction {
    @Override
    public SleepAnalysisResult analyze(List<SleepingSession> sessions) {
        return new SleepAnalysisResult("Общее количество сессий сна", (long) sessions.size());
    }
}