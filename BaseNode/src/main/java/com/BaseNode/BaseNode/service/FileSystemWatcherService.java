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

        Files.walk(uploadPath)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    try {
                        dir.register(watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE);
                    } catch (IOException e) {
                        System.out.println("[Watcher] Could not register dir: " + dir);
                    }
                });

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
                    Path watchedDir = (Path) key.watchable();
                    Path fileName = (Path) event.context();
                    Path fullPath = watchedDir.resolve(fileName);

                    try {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            if (Files.isDirectory(fullPath)) {
                                try {
                                    fullPath.register(watchService,
                                            StandardWatchEventKinds.ENTRY_CREATE,
                                            StandardWatchEventKinds.ENTRY_DELETE);
                                } catch (IOException e) {
                                    System.out.println("[Watcher] Could not register new dir: " + fullPath);
                                }
                            }
                            onFileAdded(fullPath);
                        }

                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            onFileDeleted(fullPath);
                        }
                    } catch (Exception e) {
                        System.out.println("[Watcher] Error processing event: " + e.getMessage());
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

        if (Files.isDirectory(fullPath)) {
            try {
                fullPath.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE);
                System.out.println("[Watcher] Now watching new folder: " + fullPath);
            } catch (IOException e) {
                System.out.println("[Watcher] Could not register: " + e.getMessage());
            }

            String parentPathStr = normalizePath(fullPath.getParent());
            Long parentId = folderRepository.findByFolderPath(parentPathStr)
                    .map(f -> f.getId())
                    .orElse(null);

            boolean alreadyInDB = folderRepository.findByFolderPath(normalizePath(fullPath)).isPresent();
            if (!alreadyInDB) {
                com.BaseNode.BaseNode.model.FolderEntity folder =
                        EntityFactory.createFolder(
                                fullPath.getFileName().toString(),
                                normalizePath(fullPath),
                                parentId
                        );
                folderRepository.save(folder);
                System.out.println("[Watcher] Folder added to DB: " + fullPath.getFileName());
                notifyBrowser();
            }
            return;
        }

        if (uploadingFiles.contains(fullPath.toString())) return;

        String pathStr = normalizePath(fullPath);
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

            Path parentDir = fullPath.getParent();
            Long folderId = null;
            if (!normalizePath(parentDir).equals(normalizePath(storageConfig.getUploadPath()))) {
                folderId = folderRepository.findByFolderPath(normalizePath(parentDir))
                        .map(f -> f.getId())
                        .orElseGet(() -> {
                            com.BaseNode.BaseNode.model.FolderEntity newFolder =
                                    EntityFactory.createFolder(
                                            parentDir.getFileName().toString(),
                                            normalizePath(parentDir),
                                            null
                                    );
                            return folderRepository.save(newFolder).getId();
                        });
            }

            FileEntity entity = (folderId != null)
                    ? EntityFactory.createFileInFolder(name, pathStr, size, contentType, folderId)
                    : EntityFactory.createFile(name, pathStr, size, contentType);
            fileRepository.save(entity);
            System.out.println("[Watcher] File added to DB: " + name);
            notifyBrowser();
        } catch (Exception e) {
            System.out.println("[Watcher] Could not add file: " + e.getMessage());
        }
    }

    private void onFileDeleted(Path fullPath) {
        String pathStr = normalizePath(fullPath);

        folderRepository.findByFolderPath(pathStr).ifPresent(folder -> {
            try {
                System.out.println("[Watcher] Folder deleted: " + folder.getName());

                folderRepository.findByFolderPathStartingWith(pathStr + java.io.File.separator)
                        .forEach(f -> {
                            try {
                                fileRepository.findByFolderId(f.getId()).forEach(file -> {
                                    try { fileRepository.delete(file); } catch (Exception ignored) {}
                                });
                                folderRepository.delete(f);
                            } catch (Exception ignored) {}
                        });

                fileRepository.findByFolderId(folder.getId()).forEach(file -> {
                    try { fileRepository.delete(file); } catch (Exception ignored) {}
                });
                folderRepository.delete(folder);
                notifyBrowser();
            } catch (Exception e) {
                System.out.println("[Watcher] Folder already removed from DB, skipping: " + folder.getName());
            }
        });

        fileRepository.findAll().stream()
                .filter(f -> f.getFilePath().equals(pathStr))
                .findFirst()
                .ifPresent(file -> {
                    try {
                        fileRepository.delete(file);
                        System.out.println("[Watcher] File deleted from DB: " + file.getFileName());
                        notifyBrowser();
                    } catch (Exception e) {
                        System.out.println("[Watcher] File already removed from DB, skipping: " + file.getFileName());
                    }
                });
    }

    private void syncOnStartup(Path uploadPath) throws IOException {
        System.out.println("Syncing DB with Uploads folder...");

        System.out.println("Syncing DB with Uploads folder...");

        try (java.util.stream.Stream<Path> stream = Files.walk(uploadPath)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> !p.equals(uploadPath))
                    .sorted(java.util.Comparator.comparingInt(Path::getNameCount))
                    .forEach(dir -> {
                        boolean inDB = folderRepository.findByFolderPath(normalizePath(dir)).isPresent();
                        if (inDB) return;



                        String parentPathStr = normalizePath(dir.getParent());
                        Long parentId = folderRepository.findByFolderPath(parentPathStr)
                                .map(f -> f.getId())
                                .orElse(null);

                        com.BaseNode.BaseNode.model.FolderEntity folder =
                                EntityFactory.createFolder(
                                        dir.getFileName().toString(),
                                        normalizePath(dir),
                                        parentId
                                );
                        folderRepository.save(folder);
                        System.out.println("[Sync] Added folder to DB: " + dir.getFileName());
                    });
        }


        for (FileEntity entity : fileRepository.findAll()) {
            if (!Files.exists(Path.of(entity.getFilePath()))) {
                fileRepository.delete(entity);
                System.out.println("[Sync] Removed missing file from DB: " + entity.getFileName());
            }
        }

        try (java.util.stream.Stream<Path> stream = Files.walk(uploadPath)) {
            stream.filter(p -> !Files.isDirectory(p)).forEach(file -> {
                String pathStr = normalizePath(file);
                boolean inDB = fileRepository.findAll()
                        .stream()
                        .anyMatch(f -> f.getFilePath().equals(pathStr));

                if (inDB) return;

                try {
                    String name        = file.getFileName().toString();
                    long   size        = Files.size(file);
                    String contentType = Files.probeContentType(file);
                    if (contentType == null) contentType = "application/octet-stream";

                    Path parentDir = file.getParent();
                    Long folderId = null;
                    if (!normalizePath(parentDir).equals(normalizePath(uploadPath))) {
                        folderId = folderRepository.findByFolderPath(normalizePath(parentDir))
                                .map(f -> f.getId())
                                .orElse(null);
                    }

                    FileEntity entity = (folderId != null)
                            ? EntityFactory.createFileInFolder(name, pathStr, size, contentType, folderId)
                            : EntityFactory.createFile(name, pathStr, size, contentType);
                    fileRepository.save(entity);
                    System.out.println("[Sync] Added file to DB: " + name);
                } catch (IOException e) {
                    System.out.println("[Sync] Could not add: " + e.getMessage());
                }
            });
        }

        System.out.println("Sync complete.");
    }
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
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

    private String normalizePath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

}