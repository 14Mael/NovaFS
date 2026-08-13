package io.novafs.file.service;

import io.novafs.framework.common.model.PageQuery;
import io.novafs.framework.common.model.PageResult;
import io.novafs.file.dto.FileInfoVO;

import java.io.InputStream;

/**
 * 文件信息服务（基础 CRUD）
 */
public interface FileInfoService {

    /**
     * 普通上传（小文件直接上传）
     */
    FileInfoVO upload(Long userId, Long workspaceId, Long parentId, Long storagePlatformSettingId,
                      String fileName, InputStream in);

    /**
     * 秒传落库：复用同工作空间已存在文件的存储对象，在当前目录创建记录
     */
    FileInfoVO instantUpload(Long userId, Long workspaceId, Long parentId,
                             String fileName, String md5, Long fileSize);

    /**
     * 查询目录下的文件列表
     */
    PageResult<FileInfoVO> list(Long workspaceId, Long parentId, PageQuery pageQuery);

    /**
     * 按文件名模糊搜索（工作空间范围内）
     */
    PageResult<FileInfoVO> search(Long workspaceId, String keyword, PageQuery pageQuery);

    /**
     * 下载文件
     */
    InputStream download(Long fileId, Long userId);

    /**
     * 删除文件（软删除，进回收站）
     */
    void delete(Long fileId, Long userId);

    /**
     * 回收站列表
     */
    PageResult<FileInfoVO> recycleList(Long workspaceId, PageQuery pageQuery);

    /**
     * 从回收站恢复
     */
    void restore(Long fileId, Long userId);

    /**
     * 彻底删除（含存储文件）
     */
    void purge(Long fileId, Long userId);
}
