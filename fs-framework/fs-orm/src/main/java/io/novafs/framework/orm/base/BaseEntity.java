package io.novafs.framework.orm.base;

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

    /** 主键（雪花算法） */
    @Id(keyType = com.mybatisflex.annotation.KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 创建时间 */
    @Column(onInsertValue = "NOW()")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(onInsertValue = "NOW()", onUpdateValue = "NOW()")
    private LocalDateTime updatedAt;
}
