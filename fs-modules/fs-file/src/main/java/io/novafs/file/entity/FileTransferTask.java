package io.novafs.file.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.novafs.file.enums.TransferTaskStatus;
import io.novafs.file.enums.TransferTaskType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 传输任务
 */
@Getter
@Setter
@Table("file_transfer_task")
public class FileTransferTask {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String taskId;

    private String uploadId;

    private Long fileId;

    private Long workspaceId;

    private Long userId;

    private String fileName;

    private Long fileSize;

    private String fileMd5;

    private Integer totalChunks;

    private Integer uploadedChunks;

    private Long chunkSize;

    private Long uploadedSize;

    private TransferTaskStatus status;

    private TransferTaskType taskType;

    private Long storagePlatformSettingId;

    @Column(onInsertValue = "NOW()")
    private LocalDateTime startTime;

    private LocalDateTime completeTime;

    /** 是否已完成 */
    public boolean isCompleted() {
        return status == TransferTaskStatus.COMPLETED;
    }

    /** 标记任务完成 */
    public void markCompleted() {
        this.status = TransferTaskStatus.COMPLETED;
        this.completeTime = LocalDateTime.now();
    }

    /** 标记任务失败 */
    public void markFailed() {
        this.status = TransferTaskStatus.FAILED;
        this.completeTime = LocalDateTime.now();
    }
}
