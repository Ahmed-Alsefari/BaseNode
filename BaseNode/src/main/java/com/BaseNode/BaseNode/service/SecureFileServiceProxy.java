package com.BaseNode.BaseNode.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import com.BaseNode.BaseNode.model.FileEntity;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

// move security check to this proxy class #A
public class SecureFileServiceProxy implements FileService {

    private final FileService real;
    private final HttpSession session;

    public SecureFileServiceProxy(FileService real, HttpSession session) {
        this.real = real;
        this.session = session;
    }
    // check if the user still login #A
    private void checkAuth() {
        if (session.getAttribute("loggedInUser") == null) {
            throw new SecurityException("Access denied: user not logged in.");
        }
    }

    @Override
    public FileEntity uploadFile(MultipartFile file) throws IOException {
        checkAuth();
        return real.uploadFile(file);
    }

    @Override
    public FileEntity uploadFileToFolder(MultipartFile file, Long folderId, String folderPhysicalPath) throws IOException {
        checkAuth();
        return real.uploadFileToFolder(file, folderId, folderPhysicalPath);
    }

    @Override
    public List<FileEntity> getAllFiles() {
        checkAuth();
        return real.getAllFiles();
    }

    @Override
    public List<FileEntity> getFilesByFolder(Long folderId) {
        checkAuth();
        return real.getFilesByFolder(folderId);
    }

    @Override
    public FileEntity getFile(UUID id) {
        checkAuth();
        return real.getFile(id);
    }

    @Override
    public void deleteFile(UUID id) throws IOException {
        checkAuth();
        real.deleteFile(id);
    }
}