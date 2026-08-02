package io.novafs.file.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 分片上传明细
 */
@Getter
@Setter
@Table("file_chunk_info")
public class FileChunkInfo {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String taskId;

    private String uploadId;

    private Integer chunkNumber;

    private Long chunkSize;

    private String chunkMd5;

    private String storagePath;

    private String eTag;

    @Column(onInsertValue = "NOW()")
    private LocalDateTime uploadedAt;
}
