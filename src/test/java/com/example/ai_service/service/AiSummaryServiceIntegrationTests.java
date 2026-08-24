package com.example.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ai_service.dto.AiSummaryResult;
import com.example.ai_service.dto.SummaryFormat;
import com.example.ai_service.dto.SummaryLength;

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
        AiSummaryResult result = aiSummaryService.summarize(
                "Spring Boot는 자바 기반 애플리케이션을 간단하게 실행할 수 있도록 도와준다.",
                SummaryLength.SHORT,
                SummaryFormat.KEY_POINTS
        );

        assertThat(result.summary()).isEmpty();
        assertThat(result.keyPoints()).isNotEmpty();
    }
}
