package com.nhavronskyi.progressmebackend.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LogEntryRepository extends JpaRepository<LogEntry, UUID>, JpaSpecificationExecutor<LogEntry> {

    Optional<LogEntry> findByIdAndUserId(UUID id, UUID userId);

    @Query(value = "SELECT COUNT(*) FROM entry_goals WHERE goal_id = :goalId", nativeQuery = true)
    long countEntriesByGoalId(@Param("goalId") UUID goalId);

    @Query(value = "SELECT goal_id, COUNT(*) FROM entry_goals WHERE goal_id IN :goalIds GROUP BY goal_id", nativeQuery = true)
    List<Object[]> countEntriesByGoalIds(@Param("goalIds") Collection<UUID> goalIds);

    long countByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanEqual(
            UUID userId, OffsetDateTime from, OffsetDateTime to);

    @Query(value = "SELECT type, COUNT(*) as cnt FROM log_entries " +
            "WHERE user_id = :userId AND logged_at >= :from AND logged_at <= :to " +
            "GROUP BY type", nativeQuery = true)
    List<Object[]> countByTypeInPeriod(@Param("userId") UUID userId,
                                       @Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to);

    @Query(value = "SELECT CAST(logged_at AS DATE) as log_day, COUNT(*) as cnt FROM log_entries " +
            "WHERE user_id = :userId AND logged_at >= :from AND logged_at <= :to " +
            "GROUP BY CAST(logged_at AS DATE) ORDER BY log_day", nativeQuery = true)
    List<Object[]> countByDayInPeriod(@Param("userId") UUID userId,
                                      @Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to);

    @Query(value = "SELECT CAST(logged_at AS DATE) as log_day, AVG(mood) as avg_mood FROM log_entries " +
            "WHERE user_id = :userId AND mood IS NOT NULL AND logged_at >= :from AND logged_at <= :to " +
            "GROUP BY CAST(logged_at AS DATE) ORDER BY log_day", nativeQuery = true)
    List<Object[]> avgMoodByDayInPeriod(@Param("userId") UUID userId,
                                        @Param("from") OffsetDateTime from,
                                        @Param("to") OffsetDateTime to);

    @Query(value = "SELECT CAST(logged_at AS DATE) as log_day, AVG(energy) as avg_energy FROM log_entries " +
            "WHERE user_id = :userId AND energy IS NOT NULL AND logged_at >= :from AND logged_at <= :to " +
            "GROUP BY CAST(logged_at AS DATE) ORDER BY log_day", nativeQuery = true)
    List<Object[]> avgEnergyByDayInPeriod(@Param("userId") UUID userId,
                                          @Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);

    @Query(value = "SELECT c.id, c.name, c.color, COUNT(*) as cnt " +
            "FROM entry_categories ec " +
            "JOIN categories c ON ec.category_id = c.id " +
            "JOIN log_entries le ON ec.entry_id = le.id " +
            "WHERE le.user_id = :userId AND le.logged_at >= :from AND le.logged_at <= :to " +
            "GROUP BY c.id, c.name, c.color " +
            "ORDER BY cnt DESC " +
            "FETCH FIRST 10 ROWS ONLY", nativeQuery = true)
    List<Object[]> topCategoriesInPeriod(@Param("userId") UUID userId,
                                         @Param("from") OffsetDateTime from,
                                         @Param("to") OffsetDateTime to);

    @Query(value = "SELECT AVG(mood) FROM log_entries " +
            "WHERE user_id = :userId AND mood IS NOT NULL AND logged_at >= :from AND logged_at <= :to",
            nativeQuery = true)
    Double avgMoodInPeriod(@Param("userId") UUID userId,
                           @Param("from") OffsetDateTime from,
                           @Param("to") OffsetDateTime to);

    @Query(value = "SELECT AVG(energy) FROM log_entries " +
            "WHERE user_id = :userId AND energy IS NOT NULL AND logged_at >= :from AND logged_at <= :to",
            nativeQuery = true)
    Double avgEnergyInPeriod(@Param("userId") UUID userId,
                             @Param("from") OffsetDateTime from,
                             @Param("to") OffsetDateTime to);

    @Query(value = "SELECT DISTINCT CAST(logged_at AS DATE) as log_day FROM log_entries " +
            "WHERE user_id = :userId ORDER BY log_day DESC", nativeQuery = true)
    List<Object> distinctLoggedDates(@Param("userId") UUID userId);
}
