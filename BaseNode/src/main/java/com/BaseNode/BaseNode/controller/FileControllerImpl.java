package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileControllerImpl implements FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    @Override
    public ResponseEntity<String> uploadFile(@RequestParam("file") List<MultipartFile> files) {
        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    fileService.uploadFile(file);
                }
            }
            return ResponseEntity.status(302)
                    .header("Location", "/")
                    .build();
        } catch (IOException e) {
            return ResponseEntity.status(302)
                    .header("Location", "/?error=" + e.getMessage())
                    .build();
        }
    }

    @GetMapping
    @Override
    public ResponseEntity<List<FileEntity>> listFiles() {
        return ResponseEntity.ok(fileService.getAllFiles());
    }


    @GetMapping("/view/{id}")
    public ResponseEntity<byte[]> viewFile(@PathVariable Long id) throws IOException {
        FileEntity fileEntity = fileService.getFile(id);
        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(fileEntity.getFilePath());
        byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileEntity.getFileName() + "\"")
                .body(fileContent);
    }


    @GetMapping("/download/{id}")
    @Override
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) throws IOException {
        FileEntity fileEntity = fileService.getFile(id);
        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(fileEntity.getFilePath());
        byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getFileName() + "\"")
                .body(fileContent);
    }

    @Override
    @PostMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) throws IOException {
        FileEntity fileEntity = fileService.getFile(id);
        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }
        fileService.deleteFile(id);
        return ResponseEntity.status(302)
                .header("Location", "/")
                .build();
    }

}