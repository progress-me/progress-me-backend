package com.nhavronskyi.progressmebackend.dashboard.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DashboardResponse(
        PeriodInfo period,
        EntryStats entryStats,
        MetricTrend moodTrend,
        MetricTrend energyTrend,
        GoalSummary goalSummary,
        StreakInfo streak,
        List<TopCategoryInfo> topCategories
) {

    public record PeriodInfo(int days, LocalDate from, LocalDate to) {}

    public record EntryStats(long total, Map<String, Long> byType, List<DayCount> byDay) {}

    public record DayCount(LocalDate date, long count) {}

    public record MetricTrend(double average, List<DayAverage> daily) {}

    public record DayAverage(LocalDate date, double average) {}

    public record GoalSummary(long active, long completed, long paused, List<GoalItemInfo> items) {}

    public record GoalItemInfo(UUID id, String title, String status) {}

    public record StreakInfo(int current, int longest) {}

    public record TopCategoryInfo(UUID id, String name, String color, long count) {}
}
