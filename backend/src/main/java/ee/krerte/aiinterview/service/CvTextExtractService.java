package ee.krerte.aiinterview.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class CvTextExtractService {

    public String extractText(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            boolean isPdf = filename != null && filename.toLowerCase().endsWith(".pdf");

            if (isPdf) {
                return extractFromPdf(file);
            }

            // muud failid loeme lihtsalt UTF-8 tekstina
            return new String(file.getBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            log.error("CV failist teksti lugemine ebaõnnestus", e);
            throw new IllegalStateException("CV lugemine ebaõnnestus!", e);
        }
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        // PDFBox 3.x – kasutame Loader.loadPDF(byte[])
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
