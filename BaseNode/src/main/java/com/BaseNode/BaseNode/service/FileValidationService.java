package com.BaseNode.BaseNode.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

// Validate the uploaded files using Apache Tika
// to prevent dangerous or spoofed file uploads #A
@Service
public class FileValidationService {

    private final Tika tika = new Tika();

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain"
    );

    public void validate(MultipartFile file) throws IOException {

        String detectedType =
                tika.detect(file.getInputStream());

        if (!ALLOWED_TYPES.contains(detectedType)) {

            throw new IOException(
                    "Blocked unsafe file type: " + detectedType
            );
        }
    }
}
