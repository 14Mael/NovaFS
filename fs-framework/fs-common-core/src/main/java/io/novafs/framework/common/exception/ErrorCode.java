package io.novafs.framework.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "ok"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务错误码 1000+
    USER_EXISTS(1001, "用户已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    USER_DISABLED(1003, "用户已被禁用"),
    PASSWORD_ERROR(1004, "密码错误"),
    EMAIL_EXISTS(1005, "邮箱已被使用"),
    INVALID_VERIFICATION_CODE(1006, "验证码错误或已过期"),

    FILE_NOT_FOUND(2001, "文件不存在"),
    FILE_NAME_DUPLICATE(2002, "文件名重复"),
    FILE_SIZE_EXCEEDED(2003, "文件大小超出限制"),
    STORAGE_QUOTA_EXCEEDED(2004, "存储配额不足"),
    CHUNK_MD5_MISMATCH(2005, "分片MD5校验失败"),
    CHUNK_NOT_COMPLETE(2006, "分片未上传完成"),

    WORKSPACE_NOT_FOUND(3001, "工作空间不存在"),
    WORKSPACE_SLUG_EXISTS(3002, "工作空间标识已存在"),
    NOT_WORKSPACE_MEMBER(3003, "不是工作空间成员"),
    INVITATION_EXPIRED(3004, "邀请已过期"),
    INVITATION_ALREADY_ACCEPTED(3005, "邀请已被接受"),

    STORAGE_PLATFORM_NOT_FOUND(4001, "存储平台不存在"),
    STORAGE_CONFIG_ERROR(4002, "存储配置错误"),
    STORAGE_UPLOAD_FAILED(4003, "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED(4004, "文件下载失败"),
    STORAGE_DELETE_FAILED(4005, "文件删除失败"),

    SHARE_NOT_FOUND(5001, "分享不存在"),
    SHARE_EXPIRED(5002, "分享已过期"),
    SHARE_PASSWORD_ERROR(5003, "提取码错误"),
    SHARE_VIEW_LIMIT_EXCEEDED(5004, "查看次数已达上限"),
    SHARE_DOWNLOAD_LIMIT_EXCEEDED(5005, "下载次数已达上限"),
    RAG_EMBEDDING_FAILED(6001, "向量化失败"),
    RAG_CHAT_FAILED(6002, "对话生成失败"),
    RAG_DOCUMENT_NOT_FOUND(6003, "RAG文档不存在"),
    RAG_DOCUMENT_PARSE_FAILED(6004, "文档解析失败"),
    RAG_QDRANT_UNAVAILABLE(6005, "向量库不可用");

    private final int code;
    private final String message;
}
