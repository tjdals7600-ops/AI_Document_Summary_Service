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
        validateFile(file);
        String documentText = extractText(file);
        validateDocumentText(documentText);

        String aiResponse;
        try {
            aiResponse = aiSummaryService.summarize(documentText);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("AI 요약 서비스 호출에 실패했습니다.", exception);
        }

        return new SummaryResponse(
                file.getOriginalFilename(),
                extractSummary(aiResponse),
                extractKeyPoints(aiResponse)
        );
    }

    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);

        if (lowerCaseFileName.endsWith(".pdf")) {
            return extractPdfText(file);
        }

        return extractTxtText(file);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }

        String lowerCaseFileName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerCaseFileName.endsWith(".pdf") && !lowerCaseFileName.endsWith(".txt")) {
            throw new IllegalArgumentException("PDF 또는 TXT 파일만 업로드할 수 있습니다.");
        }
    }

    private void validateDocumentText(String documentText) throws IOException {
        if (documentText.isBlank()) {
            throw new IOException("문서 내용을 읽을 수 없습니다.");
        }
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
