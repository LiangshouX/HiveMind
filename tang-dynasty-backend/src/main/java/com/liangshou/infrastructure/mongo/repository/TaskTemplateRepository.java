package com.liangshou.infrastructure.mongo.repository;

import com.liangshou.infrastructure.mongo.domain.TaskTemplateDocument;
import com.liangshou.infrastructure.mongo.domain.TaskTemplateDocument.TemplateType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 任务模板 MongoDB 数据访问接口。
 *
 * @author LiangshouX
 */
@Repository
public interface TaskTemplateRepository extends MongoRepository<TaskTemplateDocument, String> {

    /**
     * 根据模板ID查找
     */
    Optional<TaskTemplateDocument> findByTemplateId(String templateId);

    /**
     * 查找所有系统内置模板
     */
    List<TaskTemplateDocument> findByTypeOrderByCategoryAsc(TemplateType type);

    /**
     * 查找指定用户的所有自建模板
     */
    List<TaskTemplateDocument> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 查找所有模板（系统内置 + 指定用户自建）
     */
    List<TaskTemplateDocument> findByTypeOrUserIdOrderByTypeAscCategoryAsc(TemplateType type, String userId);

    /**
     * 检查模板ID是否已存在
     */
    boolean existsByTemplateId(String templateId);

    /**
     * 根据模板ID和类型删除
     */
    void deleteByTemplateIdAndType(String templateId, TemplateType type);

    /**
     * 查找指定分类的所有模板
     */
    List<TaskTemplateDocument> findByCategoryAndType(String category, TemplateType type);

    /**
     * 查找指定分类的用户模板
     */
    List<TaskTemplateDocument> findByCategoryAndUserId(String category, String userId);
}
