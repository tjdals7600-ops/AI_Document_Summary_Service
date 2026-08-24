package com.example.ai_service.dto;

import java.util.List;

public record SummaryResponse(
        String fileName,
        String summary,
        List<String> keyPoints,
        int characterCount
) {
}
