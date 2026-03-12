package com.BaseNode.BaseNode.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String path;
    private Boolean isDirectory;

    private Long size;
    private LocalDateTime lastModified;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private FileEntity parent;

    public FileEntity() {

    }

    public FileEntity(String name, String path, Boolean isDirectory, Long size, LocalDateTime lastModified, FileEntity parent) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.size = size;
        this.lastModified = lastModified;
        this.parent = parent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(Boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public FileEntity getParent() {
        return parent;
    }

    public void setParent(FileEntity parent) {
        this.parent = parent;
    }
}