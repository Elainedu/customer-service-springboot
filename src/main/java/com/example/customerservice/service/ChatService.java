// 核心業務邏輯：從 prompt.txt 載入系統提示、帶入歷史對話、呼叫本地 LLM、將結果存入 SQLite
package com.example.customerservice.service;

import com.example.customerservice.dto.ChatRequest;
import com.example.customerservice.dto.ChatResponse;
import com.example.customerservice.model.Conversation;
import com.example.customerservice.repository.ConversationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final RestClient restClient;

    // LLM 伺服器的位址，設定在 application.properties
    @Value("${llm.base-url:http://localhost:8080}")
    private String baseUrl;

    // 傳給 LLM 的 model 名稱，llama.cpp 只要填任意字串即可
    @Value("${llm.model:local-model}")
    private String model;

    // 指向 src/main/resources/prompt.txt，由 Spring 自動注入 Resource 物件
    @Value("classpath:prompt.txt")
    private Resource promptFile;

    // 讀取 prompt.txt 後存在這裡，每次對話都會當作 system 訊息傳給 LLM
    private String systemPrompt;

    // 透過建構子注入 Repository 與 RestClient.Builder（Spring Boot 自動提供）
    public ChatService(ConversationRepository conversationRepository, RestClient.Builder builder) {
        this.conversationRepository = conversationRepository;
        // 建立 HTTP 客戶端，用來呼叫 LLM API
        this.restClient = builder.build();
    }

    // Spring 容器建構完成後自動執行一次，把 prompt.txt 內容讀進 systemPrompt
    @PostConstruct
    void loadPrompt() throws IOException {
        systemPrompt = promptFile.getContentAsString(StandardCharsets.UTF_8).trim();
    }

    // 主要對話流程：取歷史 → 組訊息 → 呼叫 LLM → 儲存 → 回傳
    public ChatResponse chat(ChatRequest request) {
        // 步驟 1：從資料庫撈出這個 session 的所有歷史對話，讓 LLM 有上下文
        List<Conversation> history = conversationRepository
                .findBySessionIdOrderByCreatedAtAsc(request.getSessionId());

        // 步驟 2：組合送給 LLM 的訊息陣列
        //         順序：system prompt → 歷史對話（角色交替）→ 這次使用者的新訊息
        List<OpenAiMessage> messages = new ArrayList<>();
        messages.add(new OpenAiMessage("system", systemPrompt));
        history.forEach(c -> messages.add(new OpenAiMessage(c.getRole(), c.getContent())));
        messages.add(new OpenAiMessage("user", request.getMessage()));

        // 步驟 3：呼叫本地 LLM，取得 AI 回答
        String answer = callOpenAi(messages);

        // 步驟 4：把這輪對話（使用者問 + AI 答）分別存入 SQLite，下次呼叫時作為歷史
        Conversation userMsg = new Conversation();
        userMsg.setSessionId(request.getSessionId());
        userMsg.setRole("user");
        userMsg.setContent(request.getMessage());
        conversationRepository.save(userMsg);

        Conversation assistantMsg = new Conversation();
        assistantMsg.setSessionId(request.getSessionId());
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(answer);
        conversationRepository.save(assistantMsg);

        // 步驟 5：回傳 sessionId、AI 回答、時間戳給前端
        return new ChatResponse(request.getSessionId(), answer, LocalDateTime.now());
    }

    // 根據 sessionId 從資料庫取出對話記錄，供 GET /api/chat/history 使用
    public List<Conversation> getHistory(String sessionId) {
        return conversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    // 列出資料庫中所有不重複的 sessionId，供 GET /api/chat/sessions 使用
    public List<String> getSessions() {
        return conversationRepository.findDistinctSessionIds();
    }

    // 實際打 HTTP POST 給 LLM（llama.cpp OpenAI 相容 API）並解析回答文字
    private String callOpenAi(List<OpenAiMessage> messages) {
        // 把訊息陣列包成 LLM 要求的 JSON 格式
        OpenAiRequest body = new OpenAiRequest(model, messages);

        // 發送請求，Spring 的 RestClient 會自動將 body 序列化為 JSON
        OpenAiResponse response = restClient.post()
                .uri(baseUrl + "/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiResponse.class);  // 自動反序列化回應 JSON

        // 從回應中取出第一個 choice 的 content 文字
        return response.choices().getFirst().message().content();
    }

    // LLM 訊息格式：role（system / user / assistant）+ content（文字內容）
    record OpenAiMessage(String role, String content) {}

    // 送出給 LLM 的請求格式
    record OpenAiRequest(String model, List<OpenAiMessage> messages) {}

    // LLM 回應格式，choices 陣列通常只有一個元素
    record OpenAiResponse(List<Choice> choices) {
        record Choice(OpenAiMessage message) {}
    }
}
