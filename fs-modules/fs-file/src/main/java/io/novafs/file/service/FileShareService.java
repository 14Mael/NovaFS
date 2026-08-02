package io.novafs.file.service;

import io.novafs.file.dto.FileShareRequest;
import io.novafs.file.dto.FileShareVO;

/**
 * 文件分享服务
 */
public interface FileShareService {

    /**
     * 创建分享
     */
    FileShareVO create(Long userId, FileShareRequest request);

    /**
     * 通过分享码访问（校验提取码/过期/次数限制）
     */
    FileShareVO access(String shareCode, String password);

    /**
     * 取消分享
     */
    void cancel(Long shareId, Long userId);
}
