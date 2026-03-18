package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.model.FileEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileController {
    ResponseEntity<String> uploadFile(List<MultipartFile> files, Long folderId);
    ResponseEntity<List<FileEntity>> listFiles();
    ResponseEntity<byte[]> viewFile(Long id) throws IOException;
    ResponseEntity<byte[]> downloadFile(Long id) throws IOException;
    ResponseEntity<String> deleteFile(Long id) throws IOException;
}