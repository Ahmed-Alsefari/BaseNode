package com.BaseNode.BaseNode.service;

import com.BaseNode.BaseNode.model.FolderEntity;

import java.io.IOException;
import java.util.List;

public interface FolderService {
    FolderEntity createFolder(String name, Long parentId) throws IOException;
    List<FolderEntity> getFoldersByParent(Long parentId);
    FolderEntity getFolder(Long id);
    void deleteFolder(Long id) throws IOException;
    String buildFolderPath(Long folderId);
}
