package com.procureai.ai.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentTextExtractor {

    public String extract(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
            try (PDDocument document = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(document);
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
