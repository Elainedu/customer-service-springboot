// JPA Entity，對應資料庫 conversations 表，儲存每一則對話訊息（role: user / assistant）
package com.example.customerservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")  // 對應 SQLite 的 conversations 資料表
@Getter  // Lombok：自動產生所有欄位的 getter
@Setter  // Lombok：自動產生所有欄位的 setter
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 自動遞增的主鍵
    private Long id;

    @Column(name = "session_id", nullable = false)  // 識別同一對話的 ID，由前端產生
    private String sessionId;

    @Column(nullable = false)  // 訊息角色：user（使用者）或 assistant（AI）
    private String role;

    @Column(columnDefinition = "TEXT", nullable = false)  // 訊息內容，用 TEXT 支援較長的回答
    private String content;

    @Column(name = "created_at")  // 訊息建立時間，由 @PrePersist 自動填入
    private LocalDateTime createdAt;

    // JPA 在存入資料庫前自動呼叫，設定建立時間
    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
