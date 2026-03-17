package com.BaseNode.BaseNode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(System.getProperty("user.dir")).getParent().resolve("Uploads");
    }

    public Path getUploadPath() {
        return uploadPath;
    }
}