package io.novafs.file.dto;

import lombok.Data;

/**
 * 秒传校验结果
 */
@Data
public class CheckMd5Result {

    /** 是否已存在（可秒传） */
    private boolean exists;

    /** 已存在文件的 ID */
    private Long fileId;

    private String fileName;

    private Long fileSize;
}
