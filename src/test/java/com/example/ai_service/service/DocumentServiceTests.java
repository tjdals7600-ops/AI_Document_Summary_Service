package com.example.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import com.example.ai_service.dto.SummaryResponse;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@AutoConfigureMockMvc
class DocumentServiceTests {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiSummaryService aiSummaryService;

    @Test
    void extractsTextFromTxtFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "TXT document content".getBytes(StandardCharsets.UTF_8)
        );

        String text = documentService.extractText(file);

        assertThat(text).isEqualTo("TXT document content");
    }

    @Test
    void extractsTextFromPdfFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                createPdf("PDF document content")
        );

        String text = documentService.extractText(file);

        assertThat(text).contains("PDF document content");
    }

    @Test
    void sendsExtractedTxtTextToAiSummaryService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "TXT document content".getBytes(StandardCharsets.UTF_8)
        );
        when(aiSummaryService.summarize("TXT document content")).thenReturn("TXT summary");

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.fileName()).isEqualTo("sample.txt");
        assertThat(response.summary()).isEqualTo("TXT summary");
        assertThat(response.keyPoints()).isEmpty();
        verify(aiSummaryService).summarize("TXT document content");
    }

    @Test
    void sendsExtractedPdfTextToAiSummaryService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                createPdf("PDF document content")
        );
        when(aiSummaryService.summarize(org.mockito.ArgumentMatchers.contains("PDF document content")))
                .thenReturn("PDF summary\n- PDF key point");

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.fileName()).isEqualTo("sample.pdf");
        assertThat(response.summary()).isEqualTo("PDF summary");
        assertThat(response.keyPoints()).containsExactly("PDF key point");
        verify(aiSummaryService).summarize(org.mockito.ArgumentMatchers.contains("PDF document content"));
    }

    @Test
    void uploadsPdfAndReturnsAiSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                createPdf("Uploaded PDF content")
        );
        when(aiSummaryService.summarize(org.mockito.ArgumentMatchers.contains("Uploaded PDF content")))
                .thenReturn("AI summary result\n- First point\n* Second point\n• Third point");

        mockMvc.perform(multipart("/api/documents/summarize").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("sample.pdf"))
                .andExpect(jsonPath("$.summary").value("AI summary result"))
                .andExpect(jsonPath("$.keyPoints[0]").value("First point"))
                .andExpect(jsonPath("$.keyPoints[1]").value("Second point"))
                .andExpect(jsonPath("$.keyPoints[2]").value("Third point"));
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
