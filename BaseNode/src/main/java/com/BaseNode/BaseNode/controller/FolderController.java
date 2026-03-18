package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.model.FolderEntity;
import com.BaseNode.BaseNode.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    @Autowired
    private FolderService folderService;

    @PostMapping("/create")
    public ResponseEntity<Void> createFolder(
            @RequestParam("name") String name,
            @RequestParam(value = "parentId", required = false) Long parentId) throws IOException {
        String trimmedName = name.trim();
        if (!trimmedName.isEmpty() && !trimmedName.contains("/") && !trimmedName.contains("\\") && !trimmedName.contains("..")) {
            folderService.createFolder(trimmedName, parentId);
        }
        String redirect = parentId != null ? "/?folderId=" + parentId : "/";
        return ResponseEntity.status(302).header("Location", redirect).build();
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFolder(@PathVariable Long id) throws IOException {
        FolderEntity folder = folderService.getFolder(id);
        if (folder == null) return ResponseEntity.notFound().build();

        Path folderPath = Path.of(folder.getFolderPath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Files.walk(folderPath)
                .filter(p -> !Files.isDirectory(p))
                .forEach(p -> {
                    try {
                        zos.putNextEntry(new ZipEntry(folderPath.getParent().relativize(p).toString()));
                        Files.copy(p, zos);
                        zos.closeEntry();
                    } catch (IOException e) { throw new RuntimeException(e); }
                });
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + folder.getName() + ".zip\"")
                .body(baos.toByteArray());
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) throws IOException {
        FolderEntity folder = folderService.getFolder(id);
        if (folder == null) return ResponseEntity.status(302).header("Location", "/").build();
        Long parentId = folder.getParentId();
        folderService.deleteFolder(id);
        String redirect = parentId != null ? "/?folderId=" + parentId : "/";
        return ResponseEntity.status(302).header("Location", redirect).build();
    }
}
