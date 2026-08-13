package io.novafs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.file.entity.FileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

/**
 * 文件 Mapper
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 物理删除记录（绕过逻辑删除拦截，供回收站彻底删除使用）
     */
    @Delete("DELETE FROM file_info WHERE id = #{id}")
    int deletePhysical(@Param("id") Long id);

    /**
     * 显式更新父目录（parentId 可为 null=移回根目录，entity update 会忽略 null 字段）
     */
    @Update("UPDATE file_info SET parent_id = #{parentId} WHERE id = #{id}")
    int updateParentId(@Param("id") Long id, @Param("parentId") Long parentId);

    /**
     * 从回收站恢复（显式清空 deleted_time）
     */
    @Update("UPDATE file_info SET is_deleted = 0, deleted_time = NULL WHERE id = #{id}")
    int restoreRecord(@Param("id") Long id);
}
