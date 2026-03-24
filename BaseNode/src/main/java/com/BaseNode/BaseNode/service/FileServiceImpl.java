package com.BaseNode.BaseNode.service;

import com.BaseNode.BaseNode.config.StorageConfig;
import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.BaseNode.BaseNode.factory.EntityFactory;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private StorageConfig storageConfig;

    @Autowired
    private FileSystemWatcherService watcherService;

    @Override
    public FileEntity uploadFile(MultipartFile file) throws IOException {
        Path uploadPath = storageConfig.getUploadPath();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = file.getOriginalFilename();
        Path targetPath = uploadPath.resolve(originalName);

        int counter = 1;
        while (Files.exists(targetPath)) {
            String nameWithoutExt = originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf('.'))
                    : originalName;
            String ext = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : "";
            targetPath = uploadPath.resolve(nameWithoutExt + "_" + counter + ext);
            counter++;
        }

        String pathStr = targetPath.toString();

        watcherService.markUploading(pathStr);
        try {
            Files.copy(file.getInputStream(), targetPath);
        } finally {
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                watcherService.unmarkUploading(pathStr);
            }).start();
        }

        FileEntity entity = EntityFactory.createFile(
                targetPath.getFileName().toString(),
                pathStr,
                file.getSize(),
                file.getContentType()
        );

        return fileRepository.save(entity);
    }

    @Override
    public List<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }

    @Override
    public List<FileEntity> getFilesByFolder(Long folderId) {
        return fileRepository.findByFolderId(folderId);
    }

    @Override
    public FileEntity uploadFileToFolder(MultipartFile file, Long folderId, String folderPhysicalPath) throws IOException {
        Path uploadPath = java.nio.file.Paths.get(folderPhysicalPath);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = file.getOriginalFilename();
        Path targetPath = uploadPath.resolve(originalName);

        int counter = 1;
        while (Files.exists(targetPath)) {
            String nameWithoutExt = originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf('.'))
                    : originalName;
            String ext = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : "";
            targetPath = uploadPath.resolve(nameWithoutExt + "_" + counter + ext);
            counter++;
        }

        String pathStr = targetPath.toString();
        watcherService.markUploading(pathStr);
        try {
            Files.copy(file.getInputStream(), targetPath);
        } finally {
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                watcherService.unmarkUploading(pathStr);
            }).start();
        }

        FileEntity entity = EntityFactory.createFileInFolder(
                targetPath.getFileName().toString(),
                pathStr,
                file.getSize(),
                file.getContentType(),
                folderId
        );

        return fileRepository.save(entity);
    }

    @Override
    public FileEntity getFile(Long id) {
        return fileRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteFile(Long id) throws IOException {
        FileEntity entity = fileRepository.findById(id).orElse(null);
        if (entity != null) {
            Path filePath = Path.of(entity.getFilePath());
            Files.deleteIfExists(filePath);
            fileRepository.delete(entity);
            fileRepository.flush();
        }
    }
}