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
            // ---  (Images)  ---
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "image/svg+xml",

            // ---  PDF (Documents) ---
            "application/pdf",
            "text/plain",
            "application/rtf", // Rich Text Format

            // ---  (MS Office OpenXML) ---
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // Word (.docx)
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",         // Excel (.xlsx)
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // PowerPoint (.pptx)

            // ---  (MS Office Legacy) ---
            "application/msword",               // Word (.doc)
            "application/vnd.ms-excel",         // Excel (.xls)
            "application/vnd.ms-powerpoint",    // PowerPoint (.ppt)

            // --- (by Tika for MS Office) ---
            "application/x-tika-ooxml",    // Generic Modern Office XML (Docx, Xlsx, Pptx fallback)
            "application/x-tika-msoffice", // Generic Older Office binary fallback


            // ---  (Archives) ---
            "application/zip",
            "application/x-rar-compressed",
            "application/vnd.rar",
            "application/x-7z-compressed",

            // ---  (Data Interchange) ---
            "application/json",
            "text/csv",
            "application/xml",

            // ---  (Audio & Video) ---
            "audio/mpeg",  // MP3
            "audio/wav",   // WAV
            "video/mp4",   // MP4
            "video/mpeg"   // MPEG
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
