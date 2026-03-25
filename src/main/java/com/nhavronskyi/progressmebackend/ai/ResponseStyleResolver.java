package com.nhavronskyi.progressmebackend.ai;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResponseStyleResolver {

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

    private static final String DEFAULT_TONE_OVERLAY = TONE_OVERLAYS.get(TONE_OVERLAY.BALANCED);

    public ResponseStyle resolve(TONE_OVERLAY tone) {
        String style = TONE_OVERLAYS.getOrDefault(tone, DEFAULT_TONE_OVERLAY);

        String instructions = """
                ## Response formatting rules
                %s
                ### Formatting guidance
                - Use markdown formatting (headings, bold, bullets, tables, separators) as needed for readability.
                - Vary your response structure naturally — no two responses should look identical.
                - Adapt length and format to the content: brief when appropriate, detailed when warranted.
                - Use *italics* sparingly for softer notes or caveats.
                """.formatted(style);

        return new ResponseStyle(instructions);
    }
}
