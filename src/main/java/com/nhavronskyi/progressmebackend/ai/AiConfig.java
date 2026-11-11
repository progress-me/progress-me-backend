package com.nhavronskyi.progressmebackend.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
class AiConfig {

    @Bean
    @Qualifier("orchestratorClient")
    ChatClient orchestratorClient(ChatClient.Builder builder, AiProperties properties) {
        return builder.clone()
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(properties.models().orchestrator())
                        .temperature(0.1)
                        .maxOutputTokens(256))
                .build();
    }

    @Bean
    @Qualifier("reasoningClient")
    ChatClient reasoningClient(ChatClient.Builder builder, AiProperties properties) {
        return builder.clone()
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(properties.models().reasoning())
                        .temperature(0.7)
                        .maxOutputTokens(8192))
                .build();
    }

    @Bean
    @Qualifier("executionClient")
    ChatClient executionClient(ChatClient.Builder builder, AiProperties properties) {
        return builder.clone()
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model(properties.models().execution())
                        .temperature(0.3)
                        .maxOutputTokens(4096))
                .build();
    }
}
