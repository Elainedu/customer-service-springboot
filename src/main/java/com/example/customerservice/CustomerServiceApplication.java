// 應用程式進入點，負責啟動 Spring Boot
package com.example.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication 是三個註解的組合：
//   @Configuration        → 這個類別可以定義 Spring Bean
//   @EnableAutoConfiguration → 根據 pom.xml 的依賴自動設定（例如偵測到 JPA 就自動設定資料庫）
//   @ComponentScan        → 自動掃描同一套件下的 @Service、@Repository、@Controller 等元件
@SpringBootApplication
public class CustomerServiceApplication {

    // main 方法：整個程式從這裡開始執行
    // SpringApplication.run 會啟動 Spring 容器、載入所有設定、啟動 Tomcat Web Server
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
