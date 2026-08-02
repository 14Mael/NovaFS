package io.novafs.file.event;

import lombok.Getter;

/**
 * 文件上传完成事件
 */
@Getter
public class FileUploadCompleteEvent {

    private final Long fileId;

    private final Long userId;

    private final Long workspaceId;

    private final Long fileSize;

    public FileUploadCompleteEvent(Long fileId, Long userId, Long workspaceId, Long fileSize) {
        this.fileId = fileId;
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.fileSize = fileSize;
    }
}
