package com.codmer.turepulseai.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class DocumentTextExtractor {

    public String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Metadata metadata = new Metadata();
            metadata.set("resourceName", file.getOriginalFilename());

            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            ParseContext context = new ParseContext();
            parser.parse(inputStream, handler, metadata, context);

            String content = handler.toString();
            return content != null ? content.trim() : "";
        } catch (IOException | SAXException | TikaException e) {
            log.error("Failed to extract text from document: {}. {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new IllegalStateException("Failed to extract text from document", e);
        }
    }
}
