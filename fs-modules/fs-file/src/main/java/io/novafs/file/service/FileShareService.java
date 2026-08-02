package io.novafs.file.service;

import io.novafs.file.dto.FileShareRequest;
import io.novafs.file.dto.FileShareVO;

import java.io.InputStream;

/**
 * 文件分享服务
 */
public interface FileShareService {

    /**
     * 公开下载结果：分享信息 + 文件流
     */
    record StreamDownload(FileShareVO share, InputStream in) {
    }

    /**
     * 创建分享
     */
    FileShareVO create(Long userId, FileShareRequest request);

    /**
     * 通过分享码访问（校验提取码/过期/次数限制）
     */
    FileShareVO access(String shareCode, String password);

    /**
     * 通过分享码下载（公开访问，校验提取码/过期/次数/下载范围）
     */
    StreamDownload download(String shareCode, String password);

    /**
     * 取消分享
     */
    void cancel(Long shareId, Long userId);
}
