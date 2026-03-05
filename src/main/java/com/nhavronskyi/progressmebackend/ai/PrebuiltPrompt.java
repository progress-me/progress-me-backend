package com.nhavronskyi.progressmebackend.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PrebuiltPrompt {

    WEEKLY_ANALYSIS(
            "weekly_summary",
            "Analyze my week",
            "Analyze my progress over the past week. Identify patterns in mood, energy, and activity. " +
                    "What went well? What could improve? Give me 2-3 actionable recommendations."
    ),
    GOAL_PROGRESS(
            "analysis",
            "How am I doing on my goals?",
            "Review my active goals and recent log entries. How much progress am I making? " +
                    "Which goals need more attention? Are any at risk of falling behind?"
    ),
    PATTERN_DETECTION(
            "pattern",
            "What patterns do you see?",
            "Look at my recent entries and identify recurring patterns — in behavior, mood, energy, " +
                    "or topic. Are there any correlations I might not notice myself?"
    ),
    BURNOUT_CHECK(
            "warning",
            "Am I at risk of burnout?",
            "Assess my recent entries for signs of burnout: declining mood/energy trends, " +
                    "overwork patterns, lack of variety, or negative sentiment. Be honest but supportive."
    ),
    WEEKLY_FOCUS(
            "analysis",
            "What should I focus on this week?",
            "Based on my goals, values, and recent progress, suggest 3-5 specific focus areas for " +
                    "this week. Prioritize what will have the highest impact."
    );

    private final String insightType;
    private final String label;
    private final String query;
}
