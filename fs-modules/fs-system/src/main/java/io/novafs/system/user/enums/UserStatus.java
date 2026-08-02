package io.novafs.system.user.enums;

/**
 * 用户状态
 */
public enum UserStatus {

    /** 正常 */
    NORMAL(0),

    /** 禁用 */
    DISABLED(1);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
