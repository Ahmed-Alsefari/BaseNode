package com.BaseNode.BaseNode.composite;

import java.util.List;

public interface FileSystemNode {

    
    String getName();

    
    String getPath();

    
    long getSize();

    
    boolean isFolder();

    
    void add(FileSystemNode node);

    
    void remove(FileSystemNode node);

    
    List<FileSystemNode> getChildren();

    
    void print(String indent);
}
