package io.novafs.framework.orm.base;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 基础实体类
 */
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * 主键（雪花算法）
     * <p>雪花 ID 超出 JS 安全整数范围，序列化为字符串防止前端精度丢失</p>
     */
    @Id(keyType = com.mybatisflex.annotation.KeyType.Generator, value = KeyGenerators.snowFlakeId)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 创建时间 */
    @Column(onInsertValue = "NOW()")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(onInsertValue = "NOW()", onUpdateValue = "NOW()")
    private LocalDateTime updatedAt;
}
