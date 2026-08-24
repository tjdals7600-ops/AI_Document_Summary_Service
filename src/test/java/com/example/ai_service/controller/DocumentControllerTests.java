package com.example.ai_service.controller;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.ai_service.dto.AiSummaryResult;
import com.example.ai_service.service.AiSummaryService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@AutoConfigureMockMvc
class DocumentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiSummaryService aiSummaryService;

    @Test
    void uploadsTxtAndReturnsSummaryJson() throws Exception {
        MockMultipartFile file = textFile("sample.txt", "TXT document content");
        when(aiSummaryService.summarize("TXT document content"))
                .thenReturn(new AiSummaryResult("TXT summary", List.of("TXT key point")));

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("sample.txt"))
                .andExpect(jsonPath("$.summary").value("TXT summary"))
                .andExpect(jsonPath("$.keyPoints[0]").value("TXT key point"));
    }

    @Test
    void uploadsPdfAndReturnsSummaryJson() throws Exception {
        MockMultipartFile file = pdfFile("sample.pdf", "PDF document content");
        when(aiSummaryService.summarize(contains("PDF document content")))
                .thenReturn(new AiSummaryResult("PDF summary", List.of("PDF key point")));

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("sample.pdf"))
                .andExpect(jsonPath("$.summary").value("PDF summary"))
                .andExpect(jsonPath("$.keyPoints[0]").value("PDF key point"));
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("파일이 비어 있습니다."));
    }

    @Test
    void rejectsFileWithoutName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("파일 이름이 없습니다."));
    }

    @Test
    void rejectsUnsupportedFileExtension() throws Exception {
        MockMultipartFile file = textFile("sample.docx", "document content");

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("PDF 또는 TXT 파일만 업로드할 수 있습니다."));
    }

    @Test
    void rejectsRequestWithoutFile() throws Exception {
        mockMvc.perform(multipart("/api/documents/summarize"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("업로드할 파일이 필요합니다."));
    }

    @Test
    void returnsErrorWhenPdfCannotBeRead() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "broken.pdf",
                "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("문서 내용을 읽을 수 없습니다."));
    }

    @Test
    void returnsErrorWhenDocumentTextIsBlank() throws Exception {
        MockMultipartFile file = textFile("blank.txt", "   \n  ");

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("문서 내용을 읽을 수 없습니다."));
    }

    @Test
    void rejectsDocumentLongerThanFiftyThousandCharacters() throws Exception {
        MockMultipartFile file = textFile("large.txt", "a".repeat(50_001));

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("문서 내용은 50,000자를 초과할 수 없습니다."));
    }

    @Test
    void returnsErrorWhenAiSummaryFails() throws Exception {
        MockMultipartFile file = textFile("sample.txt", "TXT document content");
        doThrow(new RuntimeException("OpenAI error"))
                .when(aiSummaryService).summarize("TXT document content");

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI 요약 서비스 호출에 실패했습니다."));
    }

    private MockMultipartFile textFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile pdfFile(String fileName, String content) throws Exception {
        return new MockMultipartFile("file", fileName, "application/pdf", createPdf(content));
    }

    private byte[] createPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
