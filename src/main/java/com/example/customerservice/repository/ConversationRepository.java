// 資料存取層，提供依 sessionId 查詢對話記錄與列出所有 session 的方法
package com.example.customerservice.repository;

import com.example.customerservice.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// 繼承 JpaRepository 後，Spring Data JPA 會自動實作以下方法，不需要自己寫 SQL：
//   save(entity)      → INSERT 或 UPDATE
//   findById(id)      → SELECT WHERE id = ?
//   findAll()         → SELECT * FROM conversations
//   delete(entity)    → DELETE
// 泛型參數：<Conversation（操作哪張表）, Long（主鍵的型別）>
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Spring Data JPA 根據方法名稱自動產生對應的 SQL 查詢
    // findBy + SessionId  → WHERE session_id = ?
    // OrderBy + CreatedAt + Asc → ORDER BY created_at ASC（依時間從舊到新排序）
    // 用途：取出某個 session 的完整對話歷史，傳給 LLM 作為上下文
    List<Conversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // 自訂 JPQL 查詢（類似 SQL 但用 Java 類別名稱而非資料表名稱）
    // DISTINCT 確保同一個 sessionId 只出現一次
    // 用途：列出所有曾經對話過的 session 清單
    @Query("SELECT DISTINCT c.sessionId FROM Conversation c")
    List<String> findDistinctSessionIds();
}
