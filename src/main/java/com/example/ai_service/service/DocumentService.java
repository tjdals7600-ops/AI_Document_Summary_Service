package com.example.ai_service.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.ai_service.dto.SummaryResponse;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final AiSummaryService aiSummaryService;

    public DocumentService(AiSummaryService aiSummaryService) {
        this.aiSummaryService = aiSummaryService;
    }

    public SummaryResponse summarizeDocument(MultipartFile file) throws IOException {
        String documentText = extractText(file);
        String aiResponse = aiSummaryService.summarize(documentText);

        return new SummaryResponse(
                file.getOriginalFilename(),
                extractSummary(aiResponse),
                extractKeyPoints(aiResponse)
        );
    }

    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return extractPdfText(file);
        }

        return extractTxtText(file);
    }

    private String extractTxtText(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractSummary(String aiResponse) {
        List<String> summaryLines = new ArrayList<>();

        for (String line : aiResponse.lines().toList()) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty() && !isBulletPoint(trimmedLine)) {
                summaryLines.add(trimmedLine);
            }
        }

        return String.join(System.lineSeparator(), summaryLines);
    }

    private List<String> extractKeyPoints(String aiResponse) {
        List<String> keyPoints = new ArrayList<>();

        for (String line : aiResponse.lines().toList()) {
            String trimmedLine = line.trim();
            if (isBulletPoint(trimmedLine)) {
                keyPoints.add(trimmedLine.substring(1).trim());
            }
        }

        return keyPoints;
    }

    private boolean isBulletPoint(String line) {
        return line.startsWith("-") || line.startsWith("*") || line.startsWith("•");
    }
}
