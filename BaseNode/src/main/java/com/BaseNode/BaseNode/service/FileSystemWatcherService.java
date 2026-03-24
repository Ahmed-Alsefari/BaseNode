package com.BaseNode.BaseNode.service;

import com.BaseNode.BaseNode.config.StorageConfig;
import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.repository.FileRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import com.BaseNode.BaseNode.factory.EntityFactory;

@Service
public class FileSystemWatcherService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private com.BaseNode.BaseNode.repository.FolderRepository folderRepository;

    @Autowired
    private StorageConfig storageConfig;

    private WatchService watchService;
    private Thread watchThread;

    private final List<SseEmitter> emitters = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.Set<String> uploadingFiles = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    @PostConstruct
    public void start() throws IOException {

        Path uploadPath = storageConfig.getUploadPath();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        syncOnStartup(uploadPath);

        watchService = FileSystems.getDefault().newWatchService();
        uploadPath.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE);

        watchThread = new Thread(() -> {
            System.out.println("👀 Watching Uploads folder...");

            while (true) {
                WatchKey key;

                try {
                    key = watchService.take();
                } catch (InterruptedException | ClosedWatchServiceException e) {
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {

                    Path fileName = (Path) event.context();
                    Path fullPath = uploadPath.resolve(fileName);

                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        onFileAdded(fullPath);
                    }

                    if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        onFileDeleted(fullPath);
                    }
                }

                key.reset();
            }
        });

        watchThread.setDaemon(true);
        watchThread.setName("uploads-watcher");
        watchThread.start();
    }

    @PreDestroy
    public void stop() {
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            System.out.println("Error stopping watcher: " + e.getMessage());
        }
    }

    private void onFileAdded(Path fullPath) {

        if (Files.isDirectory(fullPath)) return;

        if (!fullPath.getParent().equals(storageConfig.getUploadPath())) return;
        if (uploadingFiles.contains(fullPath.toString())) return;

        String pathStr = fullPath.toString();

        boolean alreadyInDB = fileRepository.findAll()
                .stream()
                .anyMatch(f -> f.getFilePath().equals(pathStr));

        if (alreadyInDB) return;

        try {
            Thread.sleep(500);

            String name        = fullPath.getFileName().toString();
            long   size        = Files.size(fullPath);
            String contentType = Files.probeContentType(fullPath);

            if (contentType == null) contentType = "application/octet-stream";

            FileEntity entity = EntityFactory.createFile(name, pathStr, size, contentType);
            fileRepository.save(entity);
            System.out.println("[Watcher] File added to DB: " + name);
            notifyBrowser();

        } catch (Exception e) {
            System.out.println("[Watcher] Could not add file: " + e.getMessage());
        }
    }

    private void onFileDeleted(Path fullPath) {
        String pathStr = fullPath.toString();

        folderRepository.findByFolderPath(pathStr).ifPresent(folder -> {
            folderRepository.findByFolderPathStartingWith(pathStr).forEach(f -> {
                fileRepository.findByFolderId(f.getId()).forEach(fileRepository::delete);
                folderRepository.delete(f);
            });
            fileRepository.findByFolderId(folder.getId()).forEach(fileRepository::delete);
            folderRepository.delete(folder);
            System.out.println("[Watcher] Folder removed from DB: " + folder.getName());
            notifyBrowser();
            return;
        });
        for (FileEntity entity : fileRepository.findAll()) {
            if (entity.getFilePath().equals(pathStr)) {
                fileRepository.delete(entity);
                System.out.println("[Watcher] File removed from DB: " + entity.getFileName());
                notifyBrowser();
                break;
            }
        }
    }

    private void syncOnStartup(Path uploadPath) throws IOException {
        System.out.println("Syncing DB with Uploads folder...");

        for (FileEntity entity : fileRepository.findAll()) {
            if (!Files.exists(Path.of(entity.getFilePath()))) {
                fileRepository.delete(entity);
                System.out.println("[Sync] Removed missing file from DB: " + entity.getFileName());
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadPath)) {
            for (Path file : stream) {

                if (Files.isDirectory(file)) continue;

                String pathStr  = file.toString();
                boolean inDB    = fileRepository.findAll()
                        .stream()
                        .anyMatch(f -> f.getFilePath().equals(pathStr));

                if (inDB) continue;

                String name        = file.getFileName().toString();
                long   size        = Files.size(file);
                String contentType = Files.probeContentType(file);

                if (contentType == null) contentType = "application/octet-stream";

                FileEntity entity = EntityFactory.createFile(name, pathStr, size, contentType);
                fileRepository.save(entity);
                System.out.println("[Sync] Added file to DB: " + name);
            }
        }

        System.out.println("Sync complete.");
    }
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    private void notifyBrowser() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("refresh").data("reload"));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public void markUploading(String filePath) {
        uploadingFiles.add(filePath);
    }

    public void unmarkUploading(String filePath) {
        uploadingFiles.remove(filePath);
    }


}