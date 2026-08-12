package com.example.ai_service.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

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
}
