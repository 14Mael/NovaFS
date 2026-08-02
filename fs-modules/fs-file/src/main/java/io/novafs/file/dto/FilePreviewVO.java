package io.novafs.file.dto;

import lombok.Data;

/**
 * 文件预览信息
 */
@Data
public class FilePreviewVO {

    /** 预览类型：IMAGE / VIDEO / AUDIO / TEXT / PDF / UNSUPPORTED */
    private String previewType;

    /** 预览访问地址（图片/视频/PDF 等） */
    private String url;

    /** 文本内容（文本类预览） */
    private String content;

    private String fileName;
}
