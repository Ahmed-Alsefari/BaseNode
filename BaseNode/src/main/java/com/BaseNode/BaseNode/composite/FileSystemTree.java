package com.BaseNode.BaseNode.composite;

import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.model.FolderEntity;
import com.BaseNode.BaseNode.service.FileService;
import com.BaseNode.BaseNode.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileSystemTree {

    @Autowired
    private FolderService folderService;

    @Autowired
    private FileService fileService;

    
    public FolderComposite buildTree(Long parentId) {
        
        FolderEntity virtualEntity = new FolderEntity(
            parentId == null ? "root" : "folder-" + parentId,
            "/",
            null
        );
        FolderComposite rootNode = new FolderComposite(virtualEntity);

        buildRecursive(rootNode, parentId);
        return rootNode;
    }

    
    public FolderComposite buildFromFolder(Long folderId) {
        FolderEntity folderEntity = folderService.getFolder(folderId);
        if (folderEntity == null) {
            throw new IllegalArgumentException("المجلد غير موجود: id=" + folderId);
        }

        FolderComposite folderNode = new FolderComposite(folderEntity);
        buildRecursive(folderNode, folderId);
        return folderNode;
    }

    

    private void buildRecursive(FolderComposite parentNode, Long parentId) {
        
        List<FolderEntity> subFolders = folderService.getFoldersByParent(parentId);
        for (FolderEntity subFolder : subFolders) {
            FolderComposite subNode = new FolderComposite(subFolder);
            parentNode.add(subNode);
            buildRecursive(subNode, subFolder.getId());   
        }

        
        List<FileEntity> files = fileService.getFilesByFolder(parentId);
        for (FileEntity file : files) {
            parentNode.add(new FileLeaf(file));
        }
    }
}
