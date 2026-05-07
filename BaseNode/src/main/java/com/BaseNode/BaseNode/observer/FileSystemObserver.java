package com.BaseNode.BaseNode.observer;

public interface FileSystemObserver {

    void onFileAdded(String path);

    void onFileDeleted(String path);
}
