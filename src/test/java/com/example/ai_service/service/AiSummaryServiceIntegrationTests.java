package com.example.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class AiSummaryServiceIntegrationTests {

    @Autowired
    private AiSummaryService aiSummaryService;

    @Test
    void summarizesTextWithOpenAi() {
        String summary = aiSummaryService.summarize(
                "Spring Boot는 자바 기반 애플리케이션을 간단하게 실행할 수 있도록 도와준다."
        );

        assertThat(summary).isNotBlank();
    }
}
