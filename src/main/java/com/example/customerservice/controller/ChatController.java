// REST API 控制器，對外暴露 /api/chat（送訊息）、/api/chat/history（查歷史）等端點
package com.example.customerservice.controller;

import com.example.customerservice.dto.ChatRequest;
import com.example.customerservice.dto.ChatResponse;
import com.example.customerservice.model.Conversation;
import com.example.customerservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")  // 所有端點的基底路徑
@RequiredArgsConstructor      // Lombok：自動產生含 chatService 的建構子
public class ChatController {

    private final ChatService chatService;

    // POST /api/chat
    // 接收使用者訊息（JSON Body），呼叫 ChatService 處理並回傳 AI 回答
    // @Valid 會自動驗證 ChatRequest 的欄位（sessionId 和 message 不能是空白）
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }

    // GET /api/chat/history/{sessionId}
    // 回傳指定 session 的完整對話紀錄，可用來在前端重新載入歷史訊息
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<Conversation>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.getHistory(sessionId));
    }

    // GET /api/chat/sessions
    // 回傳資料庫中所有不重複的 sessionId 清單，可用來管理多個對話
    @GetMapping("/sessions")
    public ResponseEntity<List<String>> getSessions() {
        return ResponseEntity.ok(chatService.getSessions());
    }
}
