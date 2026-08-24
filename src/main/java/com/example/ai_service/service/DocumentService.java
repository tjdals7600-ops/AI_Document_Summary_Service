package com.example.ai_service.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.example.ai_service.dto.AiSummaryResult;
import com.example.ai_service.dto.SummaryFormat;
import com.example.ai_service.dto.SummaryLength;
import com.example.ai_service.dto.SummaryResponse;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_DOCUMENT_CHARACTERS = 50_000;

    private final AiSummaryService aiSummaryService;

    public DocumentService(AiSummaryService aiSummaryService) {
        this.aiSummaryService = aiSummaryService;
    }

    public SummaryResponse summarizeDocument(MultipartFile file) throws IOException {
        return summarizeDocument(file, SummaryLength.SHORT, SummaryFormat.FULL);
    }

    public SummaryResponse summarizeDocument(
            MultipartFile file,
            SummaryLength length,
            SummaryFormat format
    ) throws IOException {
        validateFile(file);
        String documentText = extractText(file);
        validateDocumentText(documentText);

        AiSummaryResult aiResult;
        try {
            aiResult = aiSummaryService.summarize(documentText, length, format);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("AI 요약 서비스 호출에 실패했습니다.", exception);
        }

        String summary = format == SummaryFormat.KEY_POINTS ? "" : aiResult.summary();
        return new SummaryResponse(
                file.getOriginalFilename(),
                summary,
                aiResult.keyPoints(),
                documentText.length()
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

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
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

        if (documentText.length() > MAX_DOCUMENT_CHARACTERS) {
            throw new IllegalArgumentException("문서 내용은 50,000자를 초과할 수 없습니다.");
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

}
