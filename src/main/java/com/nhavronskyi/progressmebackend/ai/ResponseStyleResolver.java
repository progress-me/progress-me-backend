package com.nhavronskyi.progressmebackend.ai;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResponseStyleResolver {

    private static final Map<TYPE_STRUCTURE, String> TYPE_STRUCTURES = Map.of(
            TYPE_STRUCTURE.ANALYSIS, """
                    You are providing a personal growth analysis. Freely explore what stands out in the data.""",
            TYPE_STRUCTURE.WEEKLY_SUMMARY, """
                    You are summarizing the user's week. Focus on what matters most from recent entries.""",
            TYPE_STRUCTURE.WARNING, """
                    You are raising a concern based on patterns you've noticed. Be clear about what triggered it.""",
            TYPE_STRUCTURE.PREDICTION, """
                    You are projecting where the user is heading based on their recent trajectory.""",
            TYPE_STRUCTURE.PATTERN, """
                    You are highlighting recurring patterns in the user's behavior and entries.""");

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
                ### Formatting guidance
                - Use markdown formatting (headings, bold, bullets, tables, separators) as needed for readability.
                - Vary your response structure naturally — no two responses should look identical.
                - Adapt length and format to the content: brief when appropriate, detailed when warranted.
                - Use *italics* sparingly for softer notes or caveats.
                """.formatted(structure, style);

        return new ResponseStyle(instructions);
    }
}
