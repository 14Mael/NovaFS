package io.novafs.file.service;

import io.novafs.file.dto.FileInfoVO;

/**
 * 文件夹管理服务：创建文件夹 / 重命名 / 移动
 */
public interface FileFolderService {

    /**
     * 创建文件夹（同名同目录冲突校验）
     */
    FileInfoVO createFolder(Long userId, Long workspaceId, Long parentId, String name);

    /**
     * 重命名（文件或文件夹，同目录冲突校验）
     */
    FileInfoVO rename(Long userId, Long fileId, String name);

    /**
     * 移动（文件或文件夹，禁止移入自身/子目录、防重名冲突）
     */
    FileInfoVO move(Long userId, Long fileId, Long targetParentId);
}
