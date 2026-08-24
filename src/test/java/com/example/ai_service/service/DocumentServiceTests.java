package com.example.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.ai_service.dto.AiSummaryResult;
import com.example.ai_service.dto.SummaryFormat;
import com.example.ai_service.dto.SummaryLength;
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
        when(aiSummaryService.summarize(
                "Uppercase TXT content",
                SummaryLength.SHORT,
                SummaryFormat.FULL
        ))
                .thenReturn(new AiSummaryResult("TXT summary", List.of()));

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.fileName()).isEqualTo("sample.TXT");
        assertThat(response.summary()).isEqualTo("TXT summary");
    }

    @Test
    void supportsUppercasePdfExtension() throws Exception {
        MockMultipartFile file = pdfFile("sample.PDF", "Uppercase PDF content");
        when(aiSummaryService.summarize(
                contains("Uppercase PDF content"),
                eq(SummaryLength.SHORT),
                eq(SummaryFormat.FULL)
        ))
                .thenReturn(new AiSummaryResult("PDF summary", List.of()));

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.fileName()).isEqualTo("sample.PDF");
        assertThat(response.summary()).isEqualTo("PDF summary");
    }

    @Test
    void sendsExtractedTextToAiSummaryService() throws Exception {
        MockMultipartFile file = textFile("sample.txt", "TXT document content");
        when(aiSummaryService.summarize(
                "TXT document content",
                SummaryLength.SHORT,
                SummaryFormat.FULL
        ))
                .thenReturn(new AiSummaryResult(
                        "TXT summary",
                        List.of("First point", "Second point", "Third point")
                ));

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.fileName()).isEqualTo("sample.txt");
        assertThat(response.summary()).isEqualTo("TXT summary");
        assertThat(response.keyPoints()).containsExactly("First point", "Second point", "Third point");
        assertThat(response.characterCount()).isEqualTo(20);
        verify(aiSummaryService).summarize(
                "TXT document content",
                SummaryLength.SHORT,
                SummaryFormat.FULL
        );
    }

    @Test
    void returnsEmptyKeyPointsFromStructuredAiResult() throws Exception {
        MockMultipartFile file = textFile("sample.txt", "TXT document content");
        when(aiSummaryService.summarize(
                "TXT document content",
                SummaryLength.SHORT,
                SummaryFormat.FULL
        ))
                .thenReturn(new AiSummaryResult("Summary without key points", List.of()));

        SummaryResponse response = documentService.summarizeDocument(file);

        assertThat(response.summary()).isEqualTo("Summary without key points");
        assertThat(response.keyPoints()).isEmpty();
    }

    @Test
    void returnsOnlyKeyPointsForKeyPointsFormat() throws Exception {
        MockMultipartFile file = textFile("sample.txt", "Document content");
        when(aiSummaryService.summarize(
                "Document content",
                SummaryLength.DETAILED,
                SummaryFormat.KEY_POINTS
        )).thenReturn(new AiSummaryResult("Unexpected summary", List.of("First", "Second")));

        SummaryResponse response = documentService.summarizeDocument(
                file,
                SummaryLength.DETAILED,
                SummaryFormat.KEY_POINTS
        );

        assertThat(response.summary()).isEmpty();
        assertThat(response.keyPoints()).containsExactly("First", "Second");
        assertThat(response.characterCount()).isEqualTo(16);
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                new byte[10 * 1024 * 1024 + 1]
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> documentService.summarizeDocument(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 크기는 10MB를 초과할 수 없습니다.");
    }

    @Test
    void rejectsDocumentLongerThanFiftyThousandCharacters() {
        MockMultipartFile file = textFile("large.txt", "a".repeat(50_001));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> documentService.summarizeDocument(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("문서 내용은 50,000자를 초과할 수 없습니다.");
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
