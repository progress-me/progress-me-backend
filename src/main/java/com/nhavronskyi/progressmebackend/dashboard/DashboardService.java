package com.nhavronskyi.progressmebackend.dashboard;

import com.nhavronskyi.progressmebackend.dashboard.dto.DashboardResponse;
import com.nhavronskyi.progressmebackend.dashboard.dto.DashboardResponse.*;
import com.nhavronskyi.progressmebackend.goal.GoalRepository;
import com.nhavronskyi.progressmebackend.log.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LogEntryRepository logEntryRepository;
    private final GoalRepository goalRepository;

    @Cacheable(value = "dashboard", key = "#userId + ':' + #period")
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID userId, int period) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(period);

        OffsetDateTime fromDateTime = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDateTime = to.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        PeriodInfo periodInfo = new PeriodInfo(period, from, to);
        EntryStats entryStats = buildEntryStats(userId, fromDateTime, toDateTime);
        MetricTrend moodTrend = buildMetricTrend(
                logEntryRepository.avgMoodByDayInPeriod(userId, fromDateTime, toDateTime),
                logEntryRepository.avgMoodInPeriod(userId, fromDateTime, toDateTime));
        MetricTrend energyTrend = buildMetricTrend(
                logEntryRepository.avgEnergyByDayInPeriod(userId, fromDateTime, toDateTime),
                logEntryRepository.avgEnergyInPeriod(userId, fromDateTime, toDateTime));
        GoalSummary goalSummary = buildGoalSummary(userId);
        StreakInfo streak = calculateStreak(logEntryRepository.distinctLoggedDates(userId));
        List<TopValueInfo> topValues = buildTopValues(userId, fromDateTime, toDateTime);

        return new DashboardResponse(periodInfo, entryStats, moodTrend, energyTrend,
                goalSummary, streak, topValues);
    }

    private EntryStats buildEntryStats(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        long total = logEntryRepository
                .countByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanEqual(userId, from, to);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] row : logEntryRepository.countByTypeInPeriod(userId, from, to)) {
            String type = (String) row[0];
            long count = ((Number) row[1]).longValue();
            byType.put(type, count);
        }

        List<DayCount> byDay = logEntryRepository.countByDayInPeriod(userId, from, to).stream()
                .map(row -> new DayCount(toLocalDate(row[0]), ((Number) row[1]).longValue()))
                .toList();

        return new EntryStats(total, byType, byDay);
    }

    private MetricTrend buildMetricTrend(List<Object[]> dailyRows, Double trueAverage) {
        List<DayAverage> daily = dailyRows.stream()
                .map(row -> new DayAverage(toLocalDate(row[0]), ((Number) row[1]).doubleValue()))
                .toList();

        double average = trueAverage != null ? trueAverage : 0.0;

        return new MetricTrend(average, daily);
    }

    private GoalSummary buildGoalSummary(UUID userId) {
        long active = goalRepository.countByUserIdAndStatus(userId, "active");
        long completed = goalRepository.countByUserIdAndStatus(userId, "completed");
        long paused = goalRepository.countByUserIdAndStatus(userId, "paused");

        List<GoalItemInfo> items = goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(goal -> new GoalItemInfo(goal.getId(), goal.getTitle(), goal.getStatus()))
                .toList();

        return new GoalSummary(active, completed, paused, items);
    }

    private StreakInfo calculateStreak(List<Object> dates) {
        if (dates.isEmpty()) {
            return new StreakInfo(0, 0);
        }

        List<LocalDate> sortedDates = dates.stream()
                .map(DashboardService::toLocalDate)
                .sorted(Comparator.reverseOrder())
                .distinct()
                .toList();

        // Current streak: count consecutive days from today
        int current = 0;
        LocalDate expected = LocalDate.now();
        for (LocalDate date : sortedDates) {
            if (date.equals(expected)) {
                current++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }

        // Longest streak: iterate all dates in ascending order
        List<LocalDate> asc = new ArrayList<>(sortedDates);
        Collections.sort(asc);
        int longest = 0;
        int streak = 1;
        for (int i = 1; i < asc.size(); i++) {
            if (asc.get(i).equals(asc.get(i - 1).plusDays(1))) {
                streak++;
            } else {
                longest = Math.max(longest, streak);
                streak = 1;
            }
        }
        longest = Math.max(longest, streak);

        return new StreakInfo(current, longest);
    }

    private List<TopValueInfo> buildTopValues(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        return logEntryRepository.topValuesInPeriod(userId, from, to).stream()
                .map(row -> new TopValueInfo(
                        toUUID(row[0]),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        ((Number) row[4]).longValue()
                ))
                .toList();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(value.toString());
    }

    private static UUID toUUID(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }
}
