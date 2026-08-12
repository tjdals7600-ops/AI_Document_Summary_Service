package com.example.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import com.example.ai_service.dto.SummaryResponse;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DocumentServiceTests {

    private final AiSummaryService aiSummaryService = mock(AiSummaryService.class);
    private final DocumentService documentService = new DocumentService(aiSummaryService);

    @Test
    void extractsTextFromTxtFile() throws Exception {
        MockMultipartFile file = textFile("sample.txt", "TXT document content");

        String text = documentService.extractText(file);

        assertThat(text).isEqualTo("TXT document content");
    }

    @Test
    void extractsTextFromPdfFile() throws Exception {
        MockMultipartFile file = pdfFile("sample.pdf", "PDF document content");

        String text = documentService.extractText(file);

        assertThat(text).contains("PDF document content");
    }

    @Test
    void supportsUppercaseTxtExtension() throws Exception {
        MockMultipartFile file = textFile("sample.TXT", "Uppercase TXT content");
        when(aiSummaryService.summarize("Uppercase TXT content")).thenReturn("TXT summary");

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.fileName()).isEqualTo("sample.TXT");
        assertThat(response.summary()).isEqualTo("TXT summary");
    }

    @Test
    void supportsUppercasePdfExtension() throws Exception {
        MockMultipartFile file = pdfFile("sample.PDF", "Uppercase PDF content");
        when(aiSummaryService.summarize(contains("Uppercase PDF content"))).thenReturn("PDF summary");

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.fileName()).isEqualTo("sample.PDF");
        assertThat(response.summary()).isEqualTo("PDF summary");
    }

    @Test
    void sendsExtractedTextToAiSummaryService() throws Exception {
        MockMultipartFile file = textFile("sample.txt", "TXT document content");
        when(aiSummaryService.summarize("TXT document content"))
                .thenReturn("TXT summary\n- First point\n* Second point\n• Third point");

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.fileName()).isEqualTo("sample.txt");
        assertThat(response.summary()).isEqualTo("TXT summary");
        assertThat(response.keyPoints()).containsExactly("First point", "Second point", "Third point");
        verify(aiSummaryService).summarize("TXT document content");
    }

    @Test
    void returnsEmptyKeyPointsWhenAiResponseHasNoBullets() throws Exception {
        MockMultipartFile file = textFile("sample.txt", "TXT document content");
        when(aiSummaryService.summarize("TXT document content")).thenReturn("Summary without bullets");

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.summary()).isEqualTo("Summary without bullets");
        assertThat(response.keyPoints()).isEmpty();
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
