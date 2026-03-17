package com.nhavronskyi.progressmebackend.ai;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResponseStyleResolver {

    private static final Map<TYPE_STRUCTURE, String> TYPE_STRUCTURES = Map.of(
            TYPE_STRUCTURE.ANALYSIS, """
                    ### Structure
                    - Start with a 1-2 sentence TL;DR summary.
                    - Use `## ` (H2) with an emoji for main sections: `## 🎯 Goal Progress`, `## 🧠 Patterns`, etc.
                    - Use `### ` (H3) for sub-categories: `### ✅ On Track`, `### ⚠️ Needs Attention`.
                    - Insert a blank line BEFORE and AFTER every heading and between bullet points.
                    - Use `---` separator between major sections.
                    - Every observation, recommendation, or goal MUST be its own bullet point.
                    - Keep each bullet to 1-3 sentences max. Nested details use indented sub-bullets (`  - `).
                    """,
            TYPE_STRUCTURE.WEEKLY_SUMMARY, """
                    ### Structure
                    - Open with a week-at-a-glance summary (2-3 sentences covering overall trajectory).
                    - Use a **time-segmented breakdown**: group insights by early-week / mid-week / late-week.
                    - Include a **metrics table** (markdown table) for mood, energy, and activity counts.
                    - Add a **week-over-week comparison** section highlighting changes from the prior period.
                    - Close with 2-3 concrete focus items for the coming week.
                    - Use `---` between each major section.
                    """,
            TYPE_STRUCTURE.WARNING, """
                    ### Structure
                    - Lead with **severity level** (🔴 High / 🟡 Medium / 🟢 Low) and a one-line summary.
                    - Follow with **evidence bullets** — specific entries, dates, and metrics that triggered the warning.
                    - List **immediate action items** as a numbered checklist.
                    - End with a brief supportive note acknowledging effort.
                    - Keep the entire response focused and concise — no lengthy preambles.
                    """,
            TYPE_STRUCTURE.PREDICTION, """
                    ### Structure
                    - Start with **current trajectory** — where the user is heading based on recent data.
                    - Present **projected outcomes** for 1-week and 1-month horizons.
                    - Identify **inflection points** — specific behaviors or events that could shift the trajectory.
                    - Use conditional language ("If you maintain…", "Should you shift…") for projections.
                    - Close with the single highest-leverage action the user can take.
                    """,
            TYPE_STRUCTURE.PATTERN, """
                    ### Structure
                    - Present each identified pattern as a **titled block** (`### Pattern: <name>`).
                    - Under each pattern, include: description, supporting evidence (dates/entries), and frequency.
                    - Add a **correlations** section showing relationships between patterns (e.g., "Low sleep → low energy next day").
                    - Include **confidence indicators** (🟢 Strong / 🟡 Moderate / 🔴 Weak) for each pattern.
                    - Close with which patterns to leverage and which to break.
                    """);

    private static final Map<TONE_OVERLAY, String> TONE_OVERLAYS = Map.of(
            TONE_OVERLAY.GENTLE, """
                    ### Style
                    - Use warm, narrative prose. Weave observations into an encouraging story.
                    - Use softer emojis: 🌱 growth, 💛 encouragement, 🌟 highlight, 🤗 support.
                    - Frame concerns as opportunities ("You might explore…" instead of "You failed to…").
                    - Celebrate small wins explicitly.
                    """,
            TONE_OVERLAY.STRICT, """
                    ### Style
                    - Minimal prose — let bullets and bold metrics speak.
                    - Use direct, no-nonsense language. No sugar-coating.
                    - Bold all key metrics: **7/10**, **3 days missed**, **-15% from last week**.
                    - Skip emojis except as status indicators: ✅ ❌ 📈 📉.
                    """,
            TONE_OVERLAY.ANALYTICAL, """
                    ### Style
                    - Favor data tables and percentages over narrative.
                    - Use trend indicators: 📈 📉 ➡️ for direction.
                    - Include specific numbers and comparisons wherever possible.
                    - Minimal emojis — only as data markers. Keep tone neutral and objective.
                    """,
            TONE_OVERLAY.BALANCED, """
                    ### Style
                    - Mix narrative sentences with bullet points.
                    - Moderate emoji usage: use them for section headers and key status indicators.
                    - Use **bold** for item names at the start of each bullet.
                    - Balance encouragement with honest observations.
                    """);

    @Getter
    public enum TYPE_STRUCTURE {
        ANALYSIS("analysis"),
        WEEKLY_SUMMARY("weekly_summary"),
        WARNING("warning"),
        PREDICTION("prediction"),
        PATTERN("pattern");

        private final String type;

        TYPE_STRUCTURE(String type) {
            this.type = type;
        }

        public static TYPE_STRUCTURE fromString(String value) {
            if (value == null) return ANALYSIS;
            for (TYPE_STRUCTURE ts : values()) {
                if (ts.type.equalsIgnoreCase(value)) return ts;
            }
            return ANALYSIS;
        }
    }

    @Getter
    public enum TONE_OVERLAY {
        GENTLE("gentle"),
        STRICT("strict"),
        ANALYTICAL("analytical"),
        BALANCED("balanced");

        private final String tone;

        TONE_OVERLAY(String tone) {
            this.tone = tone;
        }

        public static TONE_OVERLAY fromString(String value) {
            if (value == null) return BALANCED;
            for (TONE_OVERLAY to : values()) {
                if (to.tone.equalsIgnoreCase(value)) return to;
            }
            return BALANCED;
        }
    }

    private static final String DEFAULT_TYPE_STRUCTURE = TYPE_STRUCTURES.get(TYPE_STRUCTURE.ANALYSIS);
    private static final String DEFAULT_TONE_OVERLAY = TONE_OVERLAYS.get(TONE_OVERLAY.BALANCED);

    public ResponseStyle resolve(TYPE_STRUCTURE analysisType, TONE_OVERLAY tone) {
        String structure = TYPE_STRUCTURES.getOrDefault(analysisType, DEFAULT_TYPE_STRUCTURE);
        String style = TONE_OVERLAYS.getOrDefault(tone, DEFAULT_TONE_OVERLAY);

        String instructions = """
                ## Response formatting rules
                %s
                %s
                ### General rules
                - NEVER write walls of text without bullet points.
                - NEVER put multiple items in one paragraph — always separate them.
                - NEVER skip blank lines between bullets — cramped text is unreadable.
                - Use *italics* sparingly for softer notes or caveats.
                """.formatted(structure, style);

        return new ResponseStyle(instructions);
    }
}
