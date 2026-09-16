// 回傳給客戶端的回應格式，包含 sessionId、AI 回答內容與時間戳記
package com.example.customerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// DTO：把 ChatService 處理完的結果封裝成 JSON 傳回前端
// Spring 的 Jackson 套件會自動把這個物件序列化成以下格式的 JSON：
//   { "sessionId": "session-abc123", "answer": "您好，...", "timestamp": "2026-04-27T01:00:00" }
@Getter              // Lombok：自動產生所有欄位的 getter（Jackson 序列化時需要 getter）
@AllArgsConstructor  // Lombok：自動產生含所有欄位的建構子，讓 ChatService 能用 new ChatResponse(...) 建立物件
public class ChatResponse {

    // 對應發出請求的 sessionId，讓前端確認這則回答屬於哪個對話
    private String sessionId;

    // AI 回覆的文字內容
    private String answer;

    // 回覆產生的時間，前端可用於顯示訊息時間
    private LocalDateTime timestamp;
}
