package com.nhavronskyi.progressmebackend.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    public enum Role {
        USER, ASSISTANT
    }

    public enum ActionStatus {
        PROPOSED, CONFIRMED, REJECTED, EXECUTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "action_type", length = 30)
    private String actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_payload", columnDefinition = "jsonb")
    private Map<String, Object> actionPayload;

    @Column(name = "action_status", length = 20)
    private String actionStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ChatMessage(ChatSession session, Role role, String content) {
        this.session = session;
        this.role = role;
        this.content = content;
    }
}
