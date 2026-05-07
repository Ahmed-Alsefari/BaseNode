package com.BaseNode.BaseNode.composite;

import com.BaseNode.BaseNode.model.FolderEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FolderComposite implements FileSystemNode {

    private final FolderEntity folderEntity;
    private final List<FileSystemNode> children = new ArrayList<>();

    public FolderComposite(FolderEntity folderEntity) {
        this.folderEntity = folderEntity;
    }

    
    public FolderEntity getFolderEntity() {
        return folderEntity;
    }

    @Override
    public String getName() {
        return folderEntity.getName();
    }

    @Override
    public String getPath() {
        return folderEntity.getFolderPath();
    }

    
    @Override
    public long getSize() {
        return children.stream()
                       .mapToLong(FileSystemNode::getSize)
                       .sum();
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public void add(FileSystemNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot add a null node to the folder.");
        }
        children.add(node);
    }

    @Override
    public void remove(FileSystemNode node) {
        children.remove(node);
    }

    @Override
    public List<FileSystemNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    
    @Override
    public void print(String indent) {
        long totalSize = getSize();
        System.out.printf("%s📁 %s  (total: %,d bytes)%n", indent, getName(), totalSize);
        for (FileSystemNode child : children) {
            child.print(indent + "  ");
        }
    }

    @Override
    public String toString() {
        return "FolderComposite{name='" + getName() + "', children=" + children.size() + "}";
    }
}
