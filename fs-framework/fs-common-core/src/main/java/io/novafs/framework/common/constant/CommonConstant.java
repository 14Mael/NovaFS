package io.novafs.framework.common.constant;

/**
 * 通用常量
 */
public interface CommonConstant {

    /** 请求头 - 工作空间ID */
    String HEADER_WORKSPACE_ID = "X-Workspace-Id";

    /** 请求头 - 语言 */
    String HEADER_LANGUAGE = "Accept-Language";

    /** 日期格式 */
    String DATE_FORMAT = "yyyy-MM-dd";

    /** 日期时间格式 */
    String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** 时分秒格式 */
    String TIME_FORMAT = "HH:mm:ss";

    /** 默认分片大小 5MB */
    long DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024L;

    /** 最大文件大小 10GB */
    long MAX_FILE_SIZE = 10 * 1024L * 1024 * 1024;

    /** 分享提取码长度 */
    int SHARE_CODE_LENGTH = 6;

    /** 根节点父ID */
    String ROOT_PARENT_ID = "0";
}
