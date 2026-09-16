// 全域例外處理器，攔截驗證錯誤與 LLM API 錯誤，統一回傳 JSON 格式錯誤訊息
package com.example.customerservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.stream.Collectors;

// @RestControllerAdvice：監聽所有 Controller 拋出的例外
// 沒有這個的話，任何未處理的例外都會讓 Spring 回傳一個 HTML 格式的錯誤頁面
// 有了這個，所有錯誤都統一回傳 JSON，前端比較好處理
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 攔截 @Valid 驗證失敗時拋出的例外
    // 觸發時機：例如前端沒有帶 sessionId 或 message 欄位
    // 回傳：HTTP 400 Bad Request + { "error": "欄位名稱: 錯誤原因" }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        // 從例外中取出所有欄位的錯誤訊息，用逗號合併成一個字串
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        // 回傳 400 Bad Request，body 是 { "error": "..." }
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    // 攔截呼叫 LLM API 時，對方回傳 4xx 或 5xx 的錯誤
    // 觸發時機：例如 llama.cpp 伺服器回傳錯誤、模型不存在等
    // 把 LLM 的 HTTP 狀態碼直接透傳給前端，方便除錯
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleLlmError(RestClientResponseException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", "LLM API error: " + e.getStatusText()));
    }
}
