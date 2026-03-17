    package com.BaseNode.BaseNode.service;

    import org.springframework.web.multipart.MultipartFile;
    import java.io.IOException;
    import java.util.List;
    import com.BaseNode.BaseNode.model.FileEntity;

    public interface FileService {
        FileEntity uploadFile(MultipartFile file) throws IOException;
        List<FileEntity> getAllFiles();
        FileEntity getFile(Long id);
        void deleteFile(Long id) throws IOException;
    }