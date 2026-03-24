package com.BaseNode.BaseNode.factory;

import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.model.FolderEntity;
import com.BaseNode.BaseNode.model.UserEntity;
import com.BaseNode.BaseNode.request.LoginRequest;
import com.BaseNode.BaseNode.request.RegisterRequest;

public class EntityFactory {

    // file
    public static FileEntity createFile(String name, String path, long size, String contentType) {
        return new FileEntity(name, path, size, contentType);
    }

    public static FileEntity createFileInFolder(String name, String path, long size, String contentType, Long folderId) {
        return new FileEntity(name, path, size, contentType, folderId);
    }

    // folder
    public static FolderEntity createFolder(String name, String folderPath, Long parentId) {
        return new FolderEntity(name, folderPath, parentId);
    }

    // user
    public static UserEntity createUser(String username, String encodedPassword, String role) {
        return new UserEntity(username, encodedPassword, role);
    }

    // requests
    public static LoginRequest createLoginRequest() {
        return new LoginRequest();
    }

    public static RegisterRequest createRegisterRequest() {
        return new RegisterRequest();
    }

}