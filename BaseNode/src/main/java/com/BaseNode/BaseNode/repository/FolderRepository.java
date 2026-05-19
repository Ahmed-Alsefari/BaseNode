package com.BaseNode.BaseNode.repository;

import com.BaseNode.BaseNode.model.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long> {
    List<FolderEntity> findByParentId(Long parentId);
    List<FolderEntity> findByParentIdIsNull();
    boolean existsByNameAndParentId(String name, Long parentId);
    List<FolderEntity> findAllByFolderPath(String folderPath);
    List<FolderEntity> findByFolderPathStartingWith(String prefix);

    // Returns first match — safe even if duplicates exist in DB
    @Query("SELECT f FROM FolderEntity f WHERE f.folderPath = :path ORDER BY f.id ASC")
    List<FolderEntity> findByFolderPathOrdered(@Param("path") String folderPath);

    default Optional<FolderEntity> findByFolderPath(String folderPath) {
        List<FolderEntity> results = findByFolderPathOrdered(folderPath);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
