package com.nhavronskyi.progressmebackend.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(int maxContextEntries) {
}
