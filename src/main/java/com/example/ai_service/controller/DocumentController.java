package com.example.ai_service.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @PostMapping("/summarize")
    public String summarize(@RequestParam("file") MultipartFile file) {
        return file.getOriginalFilename();
    }
}
