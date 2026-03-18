package com.BaseNode.BaseNode.service;

import com.BaseNode.BaseNode.config.StorageConfig;
import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.model.FolderEntity;
import com.BaseNode.BaseNode.repository.FileRepository;
import com.BaseNode.BaseNode.repository.FolderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FolderServiceImpl implements FolderService {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private StorageConfig storageConfig;

    @Override
    public FolderEntity createFolder(String name, Long parentId) throws IOException {
        Path basePath = storageConfig.getUploadPath();

        String relativePath = buildRelativePath(parentId);
        Path folderPath = basePath.resolve(relativePath).resolve(name);

        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }

        FolderEntity folder = new FolderEntity(name, folderPath.toString(), parentId);
        return folderRepository.save(folder);
    }

    @Override
    public List<FolderEntity> getFoldersByParent(Long parentId) {
        if (parentId == null) {
            return folderRepository.findByParentIdIsNull();
        }
        return folderRepository.findByParentId(parentId);
    }

    @Override
    public FolderEntity getFolder(Long id) {
        return folderRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteFolder(Long id) throws IOException {
        deleteRecursive(id);
    }

    private void deleteRecursive(Long folderId) throws IOException {
        // Delete child folders recursively
        List<FolderEntity> children = folderRepository.findByParentId(folderId);
        for (FolderEntity child : children) {
            deleteRecursive(child.getId());
        }

        // Delete file records from DB
        List<FileEntity> files = fileRepository.findByFolderId(folderId);
        fileRepository.deleteAll(files);
        fileRepository.flush();

        // Delete folder record from DB
        FolderEntity folder = folderRepository.findById(folderId).orElse(null);
        if (folder != null) {
            folderRepository.delete(folder);
            folderRepository.flush();

            // Delete physical directory after DB is clean
            Path dirPath = Path.of(folder.getFolderPath());
            if (Files.exists(dirPath)) {
                deleteDirectory(dirPath);
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    @Override
    public String buildFolderPath(Long folderId) {
        if (folderId == null) return "/";

        List<String> parts = new ArrayList<>();
        Long current = folderId;

        while (current != null) {
            FolderEntity f = folderRepository.findById(current).orElse(null);
            if (f == null) break;
            parts.add(f.getName() + ":" + f.getId());
            current = f.getParentId();
        }

        Collections.reverse(parts);
        return "/" + String.join("/", parts);
    }

    private String buildRelativePath(Long parentId) {
        if (parentId == null) return "";

        List<String> parts = new ArrayList<>();
        Long current = parentId;

        while (current != null) {
            FolderEntity f = folderRepository.findById(current).orElse(null);
            if (f == null) break;
            parts.add(f.getName());
            current = f.getParentId();
        }

        Collections.reverse(parts);
        return String.join("/", parts);
    }
}

