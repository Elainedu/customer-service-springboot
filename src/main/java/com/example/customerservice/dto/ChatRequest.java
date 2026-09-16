// 客戶端送出的聊天請求格式，包含 sessionId（識別對話）與 message（使用者訊息）
package com.example.customerservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// DTO（Data Transfer Object）：專門用來接收前端傳來的 JSON，不含任何業務邏輯
// 前端送出的 JSON 範例：
//   { "sessionId": "session-abc123", "message": "我的訂單在哪裡？" }
@Getter  // Lombok：自動產生 getSessionId()、getMessage() 等 getter 方法
@Setter  // Lombok：自動產生 setSessionId()、setMessage() 等 setter 方法
public class ChatRequest {

    // 對話識別碼，由前端每次開啟頁面時隨機產生
    // 同一個 sessionId 的訊息會被串在一起當作上下文傳給 LLM
    // @NotBlank：若欄位為 null 或空白字串，Spring 會自動回傳 400 Bad Request
    @NotBlank(message = "sessionId is required")
    private String sessionId;

    // 使用者輸入的問題文字
    // @NotBlank 同樣防止空訊息被送出
    @NotBlank(message = "message is required")
    private String message;
}
