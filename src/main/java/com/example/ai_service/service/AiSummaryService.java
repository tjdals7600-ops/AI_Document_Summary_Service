package com.example.ai_service.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiSummaryService {

    private final ChatClient chatClient;

    public AiSummaryService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String summarize(String documentText) {
        String prompt = """
                아래 문서를 한국어로 이해하기 쉽게 요약해 줘.

                조건:
                - 핵심 내용을 먼저 설명
                - 중요한 내용은 bullet point로 정리
                - 불필요하게 길게 작성하지 않기

                문서:
                %s
                """.formatted(documentText);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
