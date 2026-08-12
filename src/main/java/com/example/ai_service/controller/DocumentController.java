package com.example.ai_service.controller;

import java.io.IOException;

import com.example.ai_service.service.DocumentService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/summarize")
    public String summarize(@RequestParam("file") MultipartFile file) throws IOException {
        return documentService.extractText(file);
    }
}
