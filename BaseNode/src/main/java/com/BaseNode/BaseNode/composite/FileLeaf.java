package com.BaseNode.BaseNode.composite;

import com.BaseNode.BaseNode.model.FileEntity;

import java.util.Collections;
import java.util.List;

public class FileLeaf implements FileSystemNode {

    private final FileEntity fileEntity;

    public FileLeaf(FileEntity fileEntity) {
        this.fileEntity = fileEntity;
    }

    
    public FileEntity getFileEntity() {
        return fileEntity;
    }

    @Override
    public String getName() {
        return fileEntity.getFileName();
    }

    @Override
    public String getPath() {
        return fileEntity.getFilePath();
    }

    @Override
    public long getSize() {
        return fileEntity.getFileSize();
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    
    @Override
    public void add(FileSystemNode node) {
        throw new UnsupportedOperationException(
            "لا يمكن إضافة عنصر داخل ملف: " + getName()
        );
    }

    
    @Override
    public void remove(FileSystemNode node) {
        throw new UnsupportedOperationException(
            "لا يمكن حذف عنصر من داخل ملف: " + getName()
        );
    }

    @Override
    public List<FileSystemNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public void print(String indent) {
        System.out.printf("%s📄 %s  (%,d bytes)%n", indent, getName(), getSize());
    }

    @Override
    public String toString() {
        return "FileLeaf{name='" + getName() + "', size=" + getSize() + "}";
    }
}
