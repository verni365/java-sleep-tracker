package ru.yandex.practicum.sleeptracker;

import java.util.List;

public interface SleepAnalyzer {
    SleepAnalysisResult analyze(List<SleepingSession> sessions);
}