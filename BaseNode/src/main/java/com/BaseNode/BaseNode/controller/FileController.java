package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.model.FileEntity;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface FileController {
    ResponseEntity<String> uploadFile(List<MultipartFile> files, Long folderId, HttpSession session);
    ResponseEntity<List<FileEntity>> listFiles();
    ResponseEntity<byte[]> viewFile(UUID id) throws IOException;
    ResponseEntity<byte[]> downloadFile(UUID id) throws IOException;
    ResponseEntity<String> deleteFile(UUID id, HttpSession session) throws IOException;
}