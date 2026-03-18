package com.BaseNode.BaseNode.repository;

import com.BaseNode.BaseNode.model.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long> {
    List<FolderEntity> findByParentId(Long parentId);
    List<FolderEntity> findByParentIdIsNull();
    boolean existsByNameAndParentId(String name, Long parentId);
    java.util.Optional<FolderEntity> findByFolderPath(String folderPath);
    List<FolderEntity> findByFolderPathStartingWith(String prefix);
}
