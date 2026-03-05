package com.nhavronskyi.progressmebackend.insight;

import com.nhavronskyi.progressmebackend.ai.AiContextService;
import com.nhavronskyi.progressmebackend.ai.SystemPromptBuilder;
import com.nhavronskyi.progressmebackend.ai.dto.AiContext;
import com.nhavronskyi.progressmebackend.common.ResourceNotFoundException;
import com.nhavronskyi.progressmebackend.insight.dto.AnalyzeRequest;
import com.nhavronskyi.progressmebackend.insight.dto.InsightResponse;
import com.nhavronskyi.progressmebackend.log.LogEntry;
import com.nhavronskyi.progressmebackend.log.LogEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class InsightService {

    private final InsightRepository repository;
    private final LogEntryRepository logEntryRepository;
    private final AiContextService contextService;
    private final ChatClient chatClient;
    private final SystemPromptBuilder promptBuilder;
    private final String modelName;

    public InsightService(
            InsightRepository repository,
            LogEntryRepository logEntryRepository,
            AiContextService contextService,
            ChatClient chatClient,
            SystemPromptBuilder promptBuilder,
            @Value("${spring.ai.google.genai.chat.options.model}") String modelName) {
        this.repository = repository;
        this.logEntryRepository = logEntryRepository;
        this.contextService = contextService;
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.modelName = modelName;
    }

    @Transactional
    public InsightResponse analyze(UUID userId, AnalyzeRequest request) {
        AiContext context = contextService.buildContext(userId);
        String systemPrompt = promptBuilder.build(context);

        log.info("Analyzing for user={}, type={}, query length={}",
                userId, request.resolvedType(), request.query().length());

        ChatResponse chatResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(request.query())
                .call()
                .chatResponse();

        String text = chatResponse.getResult().getOutput().getText();
        int tokens = (int) chatResponse.getMetadata().getUsage().getTotalTokens();

        log.info("generated response");

        // Re-load entries in current transaction to avoid detached entity issues
        List<UUID> entryIds = context.recentEntries().stream()
                .map(LogEntry::getId)
                .toList();
        HashSet<LogEntry> managedEntries = new HashSet<>(logEntryRepository.findAllById(entryIds));

        Insight insight = new Insight();
        insight.setUserId(userId);
        insight.setType(request.resolvedType());
        insight.setQuery(request.query());
        insight.setContent(text);
        insight.setContextSummary(promptBuilder.buildContextSummary(context));
        insight.setModel(modelName);
        insight.setTokensUsed(tokens);
        insight.setSourceEntries(managedEntries);

        return InsightResponse.from(repository.save(insight));
    }

    @Transactional(readOnly = true)
    public Page<InsightResponse> list(UUID userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(InsightResponse::from);
    }

    @Transactional(readOnly = true)
    public InsightResponse getById(UUID userId, UUID insightId) {
        return repository.findByIdAndUserId(insightId, userId)
                .map(InsightResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Insight not found"));
    }
}
