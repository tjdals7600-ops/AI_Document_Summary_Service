package com.example.ai_service.exception;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFile(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, String>> handleMissingFile(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "업로드할 파일이 필요합니다."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleFileSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("message", "파일 크기는 10MB를 초과할 수 없습니다."));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleDocumentReadFailure(IOException exception) {
        return ResponseEntity.unprocessableContent().body(Map.of("message", "문서 내용을 읽을 수 없습니다."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleAiFailure(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "AI 요약 서비스 호출에 실패했습니다."));
    }
}
